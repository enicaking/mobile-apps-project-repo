const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { logger } = require("firebase-functions");
const admin = require("firebase-admin");

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
        tokens,
      };

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