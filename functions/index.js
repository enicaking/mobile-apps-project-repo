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

      if (!fromUserId) {
        logger.warn("Missing fromUserId in study event.");
        return;
      }

      if (!subjectId) {
        logger.warn("Missing subjectId in study event.");
        return;
      }

      // 1) Load subject document
      const subjectDoc = await db.collection("subjects").doc(subjectId).get();

      if (!subjectDoc.exists) {
        logger.warn(`Subject not found: ${subjectId}`);
        return;
      }

      const subjectData = subjectDoc.data() || {};
      let memberIds = Array.isArray(subjectData.members) ? subjectData.members : [];

      if (memberIds.length === 0) {
        logger.info(`Subject ${subjectId} has no members.`);
        return;
      }

      // 2) Remove sender from notification recipients
      memberIds = memberIds.filter((uid) => uid !== fromUserId);

      if (memberIds.length === 0) {
        logger.info("No recipients left after removing sender.");
        return;
      }

      // 3) Load member user documents
      const memberDocs = await Promise.all(
        memberIds.map((uid) => db.collection("users").doc(uid).get())
      );

      // 4) Extract valid tokens
      const tokens = memberDocs
        .filter((doc) => doc.exists)
        .map((doc) => doc.data()?.fcmToken)
        .filter((token) => typeof token === "string" && token.length > 0);

      if (tokens.length === 0) {
        logger.info("No valid FCM tokens found for subject members.");
        return;
      }

      // 5) Build push message
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

      // 6) Send multicast push
      const response = await admin.messaging().sendEachForMulticast(message);

      logger.info("Notifications sent.", {
        successCount: response.successCount,
        failureCount: response.failureCount,
      });

      // 7) Remove invalid tokens from users
      const invalidTokenUserIds = [];

      response.responses.forEach((resp, index) => {
        if (!resp.success) {
          const errorCode = resp.error?.code || "";
          logger.warn(`FCM send failed for token index ${index}: ${errorCode}`);

          if (
            errorCode === "messaging/invalid-registration-token" ||
            errorCode === "messaging/registration-token-not-registered"
          ) {
            const badToken = tokens[index];
            const ownerDoc = memberDocs.find(
              (doc) => doc.exists && doc.data()?.fcmToken === badToken
            );
            if (ownerDoc) {
              invalidTokenUserIds.push(ownerDoc.id);
            }
          }
        }
      });

      await Promise.all(
        invalidTokenUserIds.map((uid) =>
          db.collection("users").doc(uid).set(
            {
              fcmToken: admin.firestore.FieldValue.delete(),
            },
            { merge: true }
          )
        )
      );
    } catch (error) {
      logger.error("Error sending study started notifications", error);
    }
  }
);