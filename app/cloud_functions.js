/**
 * Firebase Cloud Functions for Strawberrycandy
 * 
 * 1. Trigger on new chapter or novel upload -> notify readers reading the novel.
 * 2. Trigger on comment reply -> notify user A when user B replies to their comment.
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

const db = admin.firestore();

/**
 * Trigger 1: When a novel or chapter is published/updated in Realtime Database or Firestore,
 * send push notifications to users who have fcmTokens registered.
 */
exports.notifyNovelUpdate = functions.database.ref("/archive/data/novels/{novelId}")
  .onWrite(async (change, context) => {
    const novelId = context.params.novelId;
    const novelData = change.after.val();

    if (!novelData) return null; // Novel deleted

    const title = novelData.title || "New Novel / Chapter";
    const chapterTitle = novelData.chapterTitle || "New Chapter";
    const author = novelData.author || "Translator";

    const payload = {
      notification: {
        title: `📚 New Update: ${title}`,
        body: `${author} published '${chapterTitle}'. Tap to read now!`,
      },
      data: {
        novelId: novelId,
        chapterTitle: chapterTitle,
        type: "NEW_CHAPTER"
      }
    };

    try {
      const usersSnapshot = await db.collection("users").get();
      const tokens = [];
      usersSnapshot.forEach(doc => {
        const data = doc.data();
        if (data.fcmToken) {
          tokens.push(data.fcmToken);
        }
      });

      if (tokens.length === 0) {
        console.log("No FCM tokens found for novel update notification.");
        return null;
      }

      const response = await admin.messaging().sendEachForMulticast({
        tokens: tokens,
        notification: payload.notification,
        data: payload.data
      });

      console.log(`Successfully sent novel update notifications: ${response.successCount} success, ${response.failureCount} failure.`);
    } catch (error) {
      console.error("Error sending novel update notifications:", error);
    }
    return null;
  });

/**
 * Trigger 2: When a new comment or reply is added to cloud archive comments,
 * notify the parent comment author (User A) that User B replied saying "[user b] replied to your comment: [snippet]".
 */
exports.notifyCommentReply = functions.database.ref("/archive/data/comments/{commentKey}")
  .onCreate(async (snapshot, context) => {
    const comment = snapshot.val();
    if (!comment) return null;

    const parentCommentId = comment.parentCommentId;
    if (!parentCommentId) return null; // Not a reply

    const replyAuthor = comment.readerName || "A reader";
    const replyText = comment.commentText || "";
    const novelId = comment.novelId;
    const chapterTitle = comment.chapterTitle;

    try {
      const archiveRef = db.ref("/archive/data/comments");
      const archiveSnap = await archiveRef.once("value");
      const comments = archiveSnap.val();

      let parentEmail = null;
      if (comments) {
        for (const key of Object.keys(comments)) {
          const c = comments[key];
          if (c.id === parentCommentId) {
            parentEmail = c.readerEmail;
            break;
          }
        }
      }

      if (!parentEmail) {
        console.log("Parent comment author email not found for comment reply notification.");
        return null;
      }

      const userDoc = await db.collection("users").doc(parentEmail.trim().lowercase()).get();
      if (!userDoc.exists) {
        console.log(`User document not found for email: ${parentEmail}`);
        return null;
      }

      const userData = userDoc.data();
      const fcmToken = userData.fcmToken;
      if (!fcmToken) {
        console.log(`FCM token not found for user: ${parentEmail}`);
        return null;
      }

      const snippet = replyText.length > 80 ? replyText.substring(0, 80) + "..." : replyText;
      const message = {
        token: fcmToken,
        notification: {
          title: `${replyAuthor} replied to your comment`,
          body: `${replyAuthor} replied to your comment: "${snippet}"`
        },
        data: {
          novelId: novelId || "",
          chapterTitle: chapterTitle || "",
          commentId: parentCommentId,
          type: "COMMENT_REPLY"
        }
      };

      const response = await admin.messaging().send(message);
      console.log("Successfully sent comment reply notification:", response);
    } catch (error) {
      console.error("Error sending comment reply notification:", error);
    }
    return null;
  });
