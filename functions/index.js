const { logger } = require("firebase-functions");
const admin = require("firebase-admin");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");

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

      logger.log("Subject data:", subject);
      logger.log("Subject members:", subject.members);
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

exports.sendFinalGradeAddedNotification = onDocumentUpdated(
  "exams/{examId}",

  async (event) => {
    logger.log("sendFinalGradeAddedNotification triggered");

    const before = event.data.before.data();
    const after = event.data.after.data();

    const beforeActualGrades = before.actualGrades || {};
    const afterActualGrades = after.actualGrades || {};

    logger.log("Before actualGrades:", beforeActualGrades);
    logger.log("After actualGrades:", afterActualGrades);

    const addedUserIds = Object.keys(afterActualGrades).filter((uid) => {
      const oldValue = beforeActualGrades[uid];
      const newValue = afterActualGrades[uid];

      return (
        (oldValue === undefined || oldValue === null || oldValue === "") &&
        (newValue !== undefined && newValue !== null && newValue !== "")
      );
    });

    logger.log("Added final grade user ids:", addedUserIds);

    if (addedUserIds.length === 0) {
      logger.log("No new final grade added.");
      return;
    }

    const examId = event.params.examId;
    const subjectId = after.subjectId;
    const examTitle = after.title || "an exam";

    if (!subjectId) {
      logger.log("Missing subjectId in exam.");
      return;
    }

    const subjectSnap = await admin.firestore()
      .collection("subjects")
      .doc(subjectId)
      .get();

    if (!subjectSnap.exists) {
      logger.log("Subject not found:", subjectId);
      return;
    }

    const subject = subjectSnap.data() || {};
    const subjectName = subject.name || "your subject";
    const memberIds = subject.members || [];

    logger.log(`Subject ${subjectId} has ${memberIds.length} members`);

    for (const addedUserId of addedUserIds) {
      let userName = "Someone";

      const userSnap = await admin.firestore()
        .collection("users")
        .doc(addedUserId)
        .get();

      if (userSnap.exists) {
        const user = userSnap.data() || {};
        userName =
          user.displayName ||
          user.fullName ||
          user.username ||
          user.name ||
          "Someone";
      }

      const tokens = [];

      for (const memberId of memberIds) {
        const userDoc = await admin.firestore()
          .collection("users")
          .doc(memberId)
          .get();

        if (userDoc.exists) {
          const userData = userDoc.data() || {};
          if (userData.fcmToken) {
            tokens.push(userData.fcmToken);
          }
        }
      }

      logger.log("Valid tokens found:", tokens.length);

      if (tokens.length === 0) {
        logger.log("No tokens to send.");
        return;
      }

      const title = "Final grade added";
      const body = `${userName} has entered a final grade for ${examTitle} in ${subjectName}`;

      const message = {
        tokens,
        notification: {
          title,
          body,
        },
        data: {
          type: "final_grade_added",
          title,
          body,
          examId: String(examId),
          examTitle: String(examTitle),
          subjectId: String(subjectId),
          subjectName: String(subjectName),
          userId: String(addedUserId),
          userName: String(userName),
        },
           android: {
             priority: "high",
           },
      };

      logger.log("=== FINAL_GRADE DEBUG ===");
      logger.log("Title:", title);
      logger.log("Body:", body);
      logger.log("Data:", message.data);

      const response = await admin.messaging().sendEachForMulticast(message);

      logger.log("Final grade notification sent", {
        successCount: response.successCount,
        failureCount: response.failureCount,
      });
    }
  }
);