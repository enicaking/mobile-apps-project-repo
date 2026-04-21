const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");
const { onDocumentUpdated } = require("firebase-functions/v2/firestore");

admin.initializeApp();

const db = admin.firestore();

exports.sendStudyStartedNotification = onDocumentCreated(
  "study_events/{eventId}",
  async (event) => {
    try {
      const snapshot = event.data;
      if (!snapshot) {
        logger.warn("No event data found.");
        return;
      }

      const studyEvent = snapshot.data();

      const fromUserId = studyEvent.fromUserId;
      const fromUserName = studyEvent.fromUserName || "Someone";
      const subjectId = studyEvent.subjectId;
      const subjectName = studyEvent.subjectName || "Subject";
      const examTitle = studyEvent.examTitle || "Exam";

      if (!fromUserId || !subjectId) {
        logger.warn("Missing fromUserId or subjectId.");
        return;
      }

      const subjectDoc = await db.collection("subjects").doc(subjectId).get();
      if (!subjectDoc.exists) {
        logger.warn(`Subject not found: ${subjectId}`);
        return;
      }

      const subjectData = subjectDoc.data() || {};
      let memberIds = Array.isArray(subjectData.members) ? subjectData.members : [];

      memberIds = memberIds.filter((uid) => uid !== fromUserId);

      logger.info(`Subject ${subjectId} has ${memberIds.length} recipients`);

      if (memberIds.length === 0) {
        logger.info("No recipients found.");
        return;
      }

      const memberDocs = await Promise.all(
        memberIds.map((uid) => db.collection("users").doc(uid).get())
      );

      const tokens = memberDocs
        .filter((doc) => doc.exists)
        .map((doc) => doc.data()?.fcmToken)
        .filter((token) => typeof token === "string" && token.length > 0);

      logger.info(`Found ${tokens.length} valid tokens`);

      if (tokens.length === 0) {
        logger.info("No valid FCM tokens found.");
        return;
      }

      const message = {
        notification: {
          title: `${fromUserName} started studying`,
          body: `${subjectName} • ${examTitle}`,
        },
        data: {
          type: "study_started",
          fromUserId,
          fromUserName,
          subjectId,
          subjectName,
          examTitle,
        },
        android: {
          priority: "high"
        },
        tokens,
      };;

      const response = await admin.messaging().sendEachForMulticast(message);

      logger.info("Notifications sent.", {
        successCount: response.successCount,
        failureCount: response.failureCount,
      });
    } catch (error) {
      logger.error("Error sending study started notifications", error);
    }
  }
);

exports.sendRankingOvertakeNotification = onDocumentUpdated(
  "subjects/{subjectId}/ranking/{userId}",
  async (event) => {
    const before = event.data.before.data();
    const after = event.data.after.data();

    if (!before || !after) {
      logger.log("Missing before/after data");
      return;
    }

    const beforePosition = before.position;
    const afterPosition = after.position;

    if (typeof beforePosition !== "number" || typeof afterPosition !== "number") {
      logger.log("Position fields are missing or not numeric");
      return;
    }

    // Only notify when the user improves position (e.g. 4 -> 3, 3 -> 1)
    if (afterPosition >= beforePosition) {
      logger.log("No ranking improvement detected");
      return;
    }

    const subjectId = event.params.subjectId;
    const userId = event.params.userId;

    const overtakerName =
      after.userName ||
      after.displayName ||
      after.name ||
      "Someone";

    // Optional: read subject title for a nicer message
    let subjectName = subjectId;
    try {
      const subjectDoc = await admin.firestore()
        .collection("subjects")
        .doc(subjectId)
        .get();

      if (subjectDoc.exists) {
        const subjectData = subjectDoc.data() || {};
        subjectName = subjectData.name || subjectData.title || subjectId;
      }
    } catch (error) {
      logger.error("Could not read subject name", error);
    }

    // Topic must avoid spaces; subjectId is safest
    const topic = `subject_${subjectId}`;

    const title = "Ranking update";
    const body =
      `${overtakerName} moved from #${beforePosition} to #${afterPosition} in ${subjectName}`;

    const message = {
      topic,
      notification: {
        title,
        body,
      },
      data: {
        type: "ranking_overtake",
        title,
        body,
        subjectId,
        subjectName,
        overtakerId: userId,
        overtakerName,
        oldPosition: String(beforePosition),
        newPosition: String(afterPosition),
      },
    };

    try {
      const response = await admin.messaging().send(message);
      logger.log("Ranking notification sent", response);
    } catch (error) {
      logger.error("Error sending ranking notification", error);
    }
  }
);