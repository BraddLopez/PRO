/* eslint-disable no-undef, no-unused-vars */
const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.notificarRespuestaComentario = functions.firestore
    .document('comments/{commentId}')
    .onCreate((snap, context) => {
        const newComment = snap.data();
        const parentId = newComment.parentId;  // Obtener el comentario original al que se está respondiendo
        const userId = newComment.userId;  // El ID del usuario que está respondiendo

        if (parentId) {
            // Obtener el comentario original
            return admin.firestore().collection('comments').doc(parentId).get()
                .then(parentDoc => {
                    const originalUserId = parentDoc.data().userId;  // El autor original del comentario
                    if (originalUserId !== userId) {
                        // Obtener el token de FCM del autor original
                        return admin.firestore().collection('users').doc(originalUserId).get()
                            .then(userDoc => {
                                const token = userDoc.data().token;
                                if (token) {
                                    // Enviar la notificación
                                    const payload = {
                                        notification: {
                                            title: '¡Te respondieron!',
                                            body: `Tu comentario recibió una respuesta: ${newComment.text}`
                                        }
                                    };
                                    return admin.messaging().sendToDevice(token, payload)
                                        .then(response => {
                                            console.log('Notificación enviada con éxito', response);
                                        })
                                        .catch(error => {
                                            console.error('Error al enviar notificación', error);
                                        });
                                }
                            });
                    }
                });
        }
    });
/* eslint-enable no-undef, no-unused-vars */