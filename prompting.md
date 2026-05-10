Listado de bugs:

1. cuando reprodusco un audio la aplicación se bloquea. no puedo detenerlo ni interactuar con algun
   otro boton de la interfaz
2. sigue persistiendo el problema de la reproducción de notas de voz, "2026-05-10 13:15:51.109
   26909-26909 AudioBubble com.app.caretrack E No se encontró archivo de audio" en logcat. Trata de
   solucionar este bug y enriquecer el log para tener mas información de por que esta sucediendo
   esto

logcat:
2026-05-10 13:15:51.109 26909-26909 AudioBubble com.app.caretrack E No se encontró archivo de audio
2026-05-10 13:15:52.494 26909-26909 AudioBubble com.app.caretrack E No se encontró archivo de audio
2026-05-10 13:15:52.709 26909-26909 AudioBubble com.app.caretrack E No se encontró archivo de audio
2026-05-10 13:15:52.932 26909-26909 AudioBubble com.app.caretrack E No se encontró archivo de audio
2026-05-10 13:15:53.164 26909-26909 AudioBubble com.app.caretrack E No se encontró archivo de audio
2026-05-10 13:15:53.382 26909-26909 AudioBubble com.app.caretrack E No se encontró archivo de audio
2026-05-10 13:16:23.822 26909-26909 VRI[MainActivity]       com.app.caretrack D visibilityChanged
oldVisibility=true newVisibility=false
2026-05-10 13:16:23.850 26909-26931 Surface com.app.caretrack D Surface::disconnect
2026-05-10 13:16:23.851 26909-26931 BufferQueueProducer com.app.caretrack
D  [com.app.caretrack/com.app.caretrack.MainActivity#171174(BLAST Consumer)pid:26909](id:691d00000001,api:1,p:26909,c:26909)
disconnect: api 1
2026-05-10 13:21:18.514 2063-7064 PayJoyAccessService system_server E activity resuming
com.app.caretrack
2026-05-10 13:21:18.994 26909-26909 InsetsController com.app.caretrack D hide(ime(), fromIme=false)
2026-05-10 13:21:18.994 26909-26909 ImeTracker com.app.caretrack I com.app.caretrack:7b4a54d9:
onCancelled at PHASE_CLIENT_ALREADY_HIDDEN
2026-05-10 13:21:33.156 26909-26909 AudioRecorder com.app.caretrack D Audio guardado. Tamaño: 11692
bytes en /data/user/0/com.app.caretrack/files/nota_voz_1778437287369.m4a
2026-05-10 13:21:33.171 26909-26909 MediaRecorder com.app.caretrack W mediarecorder went away with
unhandled events
2026-05-10 13:21:42.756 26909-26909 VRI[MainActivity]       com.app.caretrack D visibilityChanged
oldVisibility=true newVisibility=false
2026-05-10 13:21:42.765 26909-26931 Surface com.app.caretrack D Surface::disconnect
2026-05-10 13:21:42.765 26909-26931 BufferQueueProducer com.app.caretrack
D  [com.app.caretrack/com.app.caretrack.MainActivity#171194(BLAST Consumer)pid:26909](id:691d00000002,api:1,p:26909,c:26909)