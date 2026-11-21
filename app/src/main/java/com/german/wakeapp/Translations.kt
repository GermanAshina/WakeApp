package com.german.wakeapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit

object AppLang {
    /** Idioma activo. Estado de Compose: al cambiarlo, toda la UI se repinta. */
    var language by mutableStateOf("es")

    /** Atajo de compatibilidad: true si el idioma es inglés. */
    val isEnglish: Boolean get() = language == "en"
}

fun tr(text: String, language: String = AppLang.language): String =
    when (language) {
        "en" -> EN[text] ?: text
        "fr" -> FR[text] ?: text
        else -> text
    }

/** Sustituye al Text de Material3 (mismo nombre de paquete = tiene prioridad). */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE
) {
    androidx.compose.material3.Text(
        text = tr(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        maxLines = maxLines
    )
}

private val EN: Map<String, String> = mapOf(

    // ---------- Barra inferior ----------
    "Alarmas" to "Alarms",
    "Progreso" to "Progress",
    "Tienda" to "Shop",
    "Personalización" to "Personalisation",
    "Ajustes" to "Settings",

    // ---------- Cabecera y diálogos generales ----------
    "Nivel" to "Level",
    "Recompensas" to "Rewards",
    "Recompensas de Nivel" to "Level Rewards",
    "Sube de nivel completando desafíos para ganar recompensas directas. Las monedas (🪙) solo se obtienen al desbloquear Logros:" to
            "Level up by completing challenges to earn direct rewards. Coins (🪙) are only earned by unlocking Achievements:",
    "Puntos de Energía" to "Energy Points",
    "Gana +50 🪙 por cada nivel alcanzado." to "Earn +50 🪙 for every level reached.",
    "Entendido" to "Got it",
    "Cancelar" to "Cancel",
    "Cerrar" to "Close",
    "Continuar" to "Continue",
    "Siguiente" to "Next",
    "Genial" to "Brilliant",
    "Perfil" to "Profile",
    "Próximamente..." to "Coming soon...",
    "¿Qué es WakeApp?" to "What is WakeApp?",
    "WakeApp es tu despertador inteligente. En lugar de un simple botón, cada alarma te lanza desafíos mentales o físicos que debes superar para desactivarla." to
            "WakeApp is your smart alarm clock. Instead of a simple button, every alarm throws mental or physical challenges at you that you must beat to switch it off.",

    // ---------- Alarmas ----------
    "No hay alarmas próximas" to "No upcoming alarms",
    "Pulsa el botón + para crear una" to "Tap the + button to create one",
    "Límite alcanzado" to "Limit reached",
    "Has alcanzado el límite de 4 alarmas. Ve a la Tienda y compra 'Alarmas Infinitas' para crear más." to
            "You have reached the limit of 4 alarms. Go to the Shop and buy 'Infinite Alarms' to create more.",
    "Ir a la Tienda" to "Go to the Shop",
    "Eliminar alarma" to "Delete alarm",
    "Eliminar" to "Delete",
    "Eliminar Alarma" to "Delete Alarm",
    "Editar Alarma" to "Edit Alarm",
    "Nueva Alarma" to "New Alarm",
    "Guardar Alarma" to "Save Alarm",
    "Guardar Cambios" to "Save Changes",
    "¿Para qué es esta alarma?" to "What is this alarm for?",
    "Trabajo, Instituto, Gym..." to "Work, School, Gym...",
    "Color" to "Colour",
    "Color de la Alarma" to "Alarm Colour",
    "Minijuego" to "Minigame",
    "Rondas" to "Rounds",
    "rondas" to "rounds",
    "Dificultad" to "Difficulty",
    "Sonido" to "Sound",
    "Predeterminado" to "Default",
    "Sonido personalizado" to "Custom sound",
    "Elegir archivo de audio" to "Choose audio file",

    // ---------- Minijuegos (nombres) ----------
    "Cálculo" to "Maths",
    "Frase" to "Phrase",
    "Delinear" to "Trace",

    // ---------- Progreso y logros ----------
    "Tu Progreso" to "Your Progress",
    "Días Distintos" to "Distinct Days",
    "días" to "days",
    "LOGROS" to "ACHIEVEMENTS",
    "¡LOGRO DESBLOQUEADO!" to "ACHIEVEMENT UNLOCKED!",
    "¡NIVEL SUBIDO!" to "LEVEL UP!",
    "¡COMPLETADO!" to "COMPLETE!",
    "+1 NIVEL" to "+1 LEVEL",
    "PUNTOS" to "POINTS",
    "Puntos" to "Points",

    "Una alarma al año no hace daño" to "One alarm a year does no harm",
    "Despierta por primera vez." to "Wake up for the first time.",
    "Tres son multitud" to "Three's a crowd",
    "3 días distintos despertando." to "Wake up on 3 different days.",
    "No hay semana sin siete días" to "No week without seven days",
    "7 días distintos despertando." to "Wake up on 7 different days.",
    "Nuevo Hábito Adquirido" to "New Habit Acquired",
    "21 días distintos. Ya es rutina." to "21 different days. It's a routine now.",
    "Mes de Hierro" to "Iron Month",
    "30 días distintos despertando." to "Wake up on 30 different days.",
    "Ambicioso" to "Ambitious",
    "100 días distintos. Leyenda." to "100 different days. Legend.",
    "2x2=?" to "2x2=?",
    "Completa 10 desafíos de matemáticas." to "Complete 10 maths challenges.",
    "Despacito y con buena letra" to "Slow and neat",
    "Completa 10 desafíos de escribir." to "Complete 10 writing challenges.",
    "No te pases de la raya" to "Stay within the lines",
    "Completa 10 desafíos de delinear." to "Complete 10 tracing challenges.",
    "Cuádruple o nada" to "Quadruple or nothing",
    "Supera una alarma de 4 rondas." to "Beat a 4-round alarm.",
    "Madrugador" to "Early Bird",
    "Levántate a las 7:00h o antes." to "Get up at 7:00 AM or earlier.",
    "Comprador" to "Shopper",
    "Compra un objeto en la Tienda." to "Buy an item in the Shop.",
    "Coleccionista" to "Collector",
    "Compra 10 objetos en la Tienda." to "Buy 10 items in the Shop.",
    "Avaricioso" to "Greedy",
    "Acumula 5,000 puntos." to "Collect 5,000 points.",
    "Caja Tonta" to "Idiot Box",
    "Ve un anuncio recompensado." to "Watch a rewarded ad.",

    // ---------- Tienda ----------
    "Mejoras permanentes" to "Permanent upgrades",
    "🧪 Pruebas: +50 🪙" to "🧪 Test: +50 🪙",
    "MEJORAS INDIVIDUALES" to "INDIVIDUAL UPGRADES",
    "Alarmas Infinitas" to "Infinite Alarms",
    "Crea todas las alarmas que quieras sin límite." to "Create as many alarms as you like, with no limit.",
    "Nuevos Marcadores" to "New Markers",
    "Desbloquea 10 colores para organizar tus alarmas." to "Unlock 10 colours to organise your alarms.",
    "Más Asaltos" to "More Rounds",
    "Aumenta el límite de rondas por alarma a 10." to "Raise the round limit per alarm to 10.",
    "Elimina todos los anuncios" to "Remove all ads",
    "Una experiencia totalmente limpia y sin interrupciones." to "A completely clean experience with no interruptions.",
    "Desbloquea tu Avatar" to "Unlock your Avatar",
    "Muestra una foto de perfil personalizada en la app." to "Show a custom profile picture in the app.",
    "Multiplicador de XP" to "XP Multiplier",
    "Gana el doble de experiencia al despertar." to "Earn double experience when you wake up.",
    "TEMAS" to "THEMES",
    "Cambia el color de toda la interfaz. Equípalo desde Ajustes." to "Changes the colour of the whole interface. Equip it from Settings.",
    "Comprar" to "Buy",
    "Comprado" to "Purchased",
    "Equipado" to "Equipped",

    // ---------- Temas ----------
    "Por defecto" to "Default",
    "Aurum Legendario" to "Legendary Aurum",
    "Rosa Cyberpunk" to "Cyberpunk Pink",
    "Verde Matrix" to "Matrix Green",

    // ---------- Ajustes ----------
    "Volumen de alarma" to "Alarm volume",
    "Volumen del sistema" to "System volume",
    "Confirmar borrado" to "Confirm deletion",
    "Preguntar antes de eliminar una alarma." to "Ask before deleting an alarm.",
    "PERSONALIZACIÓN" to "PERSONALISATION",
    "Modo claro" to "Light mode",
    "Interfaz clara activada." to "Light interface on.",
    "Interfaz oscura activada." to "Dark interface on.",
    "Sonido por defecto" to "Default sound",
    "Tono de alarma" to "Alarm tone",
    "Tono de notificación" to "Notification tone",
    "Tono de llamada" to "Ringtone",
    "Notificación" to "Notification",
    "Tema de la app" to "App theme",
    "Idioma" to "Language",
    "Cómpralo en la Tienda" to "Buy it in the Shop",

    // ---------- Onboarding ----------
    "Selecciona tu idioma" to "Choose your language",
    "¿Cómo te llamas?" to "What's your name?",
    "Usaremos tu nombre para saludarte" to "We'll use your name to greet you",
    "Tu nombre (opcional)" to "Your name (optional)",
    "¿Para qué usarás WakeApp?" to "What will you use WakeApp for?",
    "Elige tu objetivo principal" to "Choose your main goal",
    "Productividad" to "Productivity",
    "Levantarme con energía para rendir más" to "Wake up with energy to get more done",
    "Madrugar" to "Early rising",
    "Quiero levantarme más temprano" to "I want to get up earlier",
    "Disciplina" to "Discipline",
    "Obligarme a despertarme sí o sí" to "Force myself to wake up, no excuses",
    "¿Qué dificultad prefieres?" to "Which difficulty do you prefer?",
    "Será la que uses por defecto al crear alarmas" to "It will be your default when creating alarms",
    "Fácil" to "Easy",
    "Cálculos sencillos, frases cortas" to "Simple sums, short phrases",
    "Media" to "Medium",
    "Cálculos medios, frases normales" to "Medium sums, normal phrases",
    "Difícil" to "Hard",
    "Multiplicaciones, frases largas" to "Multiplications, long phrases",
    "Extrema" to "Extreme",
    "Operaciones complejas, textos exactos" to "Complex sums, exact text",
    "¿A qué hora sueles despertarte?" to "What time do you usually wake up?",
    "La usaremos como punto de partida al crear tus alarmas" to "We'll use it as a starting point when you create alarms",
    "Atrás" to "Back",
    "Empezar" to "Get started",

    // ---------- Pantalla del reto ----------
    "FRASE EXACTA" to "EXACT PHRASE",
    "CÁLCULO MENTAL" to "MENTAL MATHS",
    "Escribe la secuencia exacta que ves abajo:" to "Type the exact sequence you see below:",
    "Resuelve el cálculo para silenciar la alarma:" to "Solve the sum to silence the alarm:",
    "Respuesta" to "Answer",
    "Respuesta incorrecta. Inténtalo de nuevo." to "Wrong answer. Try again.",
    "DESACTIVAR ALARMA" to "TURN OFF ALARM",
    "DELINEADO PRECISO" to "PRECISE TRACING",
    "Traza la figura:" to "Trace the shape:",
    "Círculo Neón" to "Neon Circle",
    "Triángulo" to "Triangle",
    "Estrella" to "Star",
    "Corazón" to "Heart",
    "Onda Senoidal" to "Sine Wave",

    // ---------- Notificación de la alarma ----------
    "¡Alarma Sonando!" to "Alarm Ringing!",
    "Resolver desafío" to "Solve challenge",
    "Toca para resolver el desafío" to "Tap to solve the challenge",
    "Servicio de Alarma Activo" to "Alarm Service Active",

    // ---------- Información, Privacidad y FAQ ----------
    "GENERAL" to "GENERAL",
    "Pedir confirmación antes de eliminar una alarma." to "Ask for confirmation before deleting an alarm.",
    "Dificultad por defecto" to "Default difficulty",
    "INFORMACIÓN DE LA APP" to "APP INFO",
    "WakeApp es un despertador con desafíos interactivos diseñado para ayudarte a despertar de verdad, registrar tu avance y personalizar la app a tu gusto." to
            "WakeApp is an alarm clock with interactive challenges designed to help you truly wake up, track your progress, and customise the app to your liking.",
    "PRIVACIDAD DEL USUARIO" to "USER PRIVACY",
    "Datos y preferencias" to "Data and preferences",
    "Tus alarmas, preferencias, progreso y niveles se almacenan en tu dispositivo. Si seleccionas un archivo de audio propio, la app solo utiliza el archivo que elijas." to
            "Your alarms, preferences, progress, and levels are stored on your device. If you select your own audio file, the app only uses the file you choose.",
    "DUDAS FRECUENTES" to "FREQUENTLY ASKED QUESTIONS",
    "¿Cómo se desactiva una alarma?" to "How do you turn off an alarm?",
    "Al sonar la alarma tendrás que superar los desafíos configurados y las rondas indicadas para apagarla." to
            "When the alarm goes off, you must complete the configured challenges and the set number of rounds to turn it off.",
    "¿Qué pasa si reinicio el móvil o cambia la hora?" to "What happens if I restart my phone or the time changes?",
    "Las alarmas activas se reprograman y automáticamente con el horario de tu dispositivo." to
            "Active alarms are rescheduled automatically with your device's schedule.",
    "¿Cómo gano XP, niveles y puntos?" to "How do I earn XP, levels, and points?",
    "Completando desafíos sumas XP para subir de nivel, y al desbloquear logros obtienes puntos (🪙) para gastar en la Tienda." to
            "By completing challenges you gain XP to level up, and by unlocking achievements you earn points (🪙) to spend in the Shop.",
    "¿Puedo usar mis propios archivos de audio?" to "Can I use my own audio files?",
    "Sí, al configurar una alarma puedes elegir un archivo de audio desde tu dispositivo." to
            "Yes, when configuring an alarm you can choose an audio file from your device."
)

private val FR: Map<String, String> = mapOf(

    // ---------- Barre de navigation ----------
    "Alarmas" to "Alarmes",
    "Progreso" to "Progrès",
    "Tienda" to "Boutique",
    "Personalización" to "Personnalisation",
    "Ajustes" to "Réglages",

    // ---------- En-tête et dialogues généraux ----------
    "Nivel" to "Niveau",
    "Recompensas" to "Récompenses",
    "Recompensas de Nivel" to "Récompenses de niveau",
    "Sube de nivel completando desafíos para ganar recompensas directas. Las monedas (🪙) solo se obtienen al desbloquear Logros:" to
            "Montez de niveau en relevant des défis pour gagner des récompenses directes. Les pièces (🪙) ne s'obtiennent qu'en débloquant des succès :",
    "Puntos de Energía" to "Points d'énergie",
    "Gana +50 🪙 por cada nivel alcanzado." to "Gagnez +50 🪙 pour chaque niveau atteint.",
    "Entendido" to "Compris",
    "Cancelar" to "Annuler",
    "Cerrar" to "Fermer",
    "Continuar" to "Continuer",
    "Siguiente" to "Suivant",
    "Genial" to "Génial",
    "Perfil" to "Profil",
    "Próximamente..." to "Bientôt disponible...",
    "¿Qué es WakeApp?" to "Qu'est-ce que WakeApp ?",
    "WakeApp es tu despertador inteligente. En lugar de un simple botón, cada alarma te lanza desafíos mentales o físicos que debes superar para desactivarla." to
            "WakeApp est votre réveil intelligent. Au lieu d'un simple bouton, chaque alarme vous lance des défis mentaux ou physiques que vous devez surmonter pour la désactiver.",

    // ---------- Alarmes ----------
    "No hay alarmas próximas" to "Aucune alarme à venir",
    "Pulsa el botón + para crear una" to "Appuyez sur le bouton + pour en créer une",
    "Límite alcanzado" to "Limite atteinte",
    "Has alcanzado el límite de 4 alarmas. Ve a la Tienda y compra 'Alarmas Infinitas' para crear más." to
            "Vous avez atteint la limite de 4 alarmes. Allez dans la Boutique et achetez 'Alarmes infinies' pour en créer plus.",
    "Ir a la Tienda" to "Aller à la Boutique",
    "Eliminar alarma" to "Supprimer l'alarme",
    "Eliminar" to "Supprimer",
    "Eliminar Alarma" to "Supprimer l'alarme",
    "Editar Alarma" to "Modifier l'alarme",
    "Nueva Alarma" to "Nouvelle alarme",
    "Guardar Alarma" to "Enregistrer l'alarme",
    "Guardar Cambios" to "Enregistrer les modifications",
    "¿Para qué es esta alarma?" to "À quoi sert cette alarme ?",
    "Trabajo, Instituto, Gym..." to "Travail, Lycée, Gym...",
    "Color" to "Couleur",
    "Color de la Alarma" to "Couleur de l'alarme",
    "Minijuego" to "Mini-jeu",
    "Rondas" to "Manches",
    "rondas" to "manches",
    "Dificultad" to "Difficulté",
    "Sonido" to "Son",
    "Predeterminado" to "Par défaut",
    "Sonido personalizado" to "Son personnalisé",
    "Elegir archivo de audio" to "Choisir un fichier audio",

    // ---------- Mini-jeux ----------
    "Cálculo" to "Calcul",
    "Frase" to "Phrase",
    "Delinear" to "Tracer",

    // ---------- Progrès et succès ----------
    "Tu Progreso" to "Votre progression",
    "Días Distintos" to "Jours distincts",
    "días" to "jours",
    "LOGROS" to "SUCCÈS",
    "¡LOGRO DESBLOQUEADO!" to "SUCCÈS DÉBLOQUÉ !",
    "¡NIVEL SUBIDO!" to "NIVEAU SUPÉRIEUR !",
    "¡COMPLETADO!" to "TERMINÉ !",
    "+1 NIVEL" to "+1 NIVEAU",
    "PUNTOS" to "POINTS",
    "Puntos" to "Points",

    "Una alarma al año no hace daño" to "Une alarme par an ne fait pas de mal",
    "Despierta por primera vez." to "Réveillez-vous pour la première fois.",
    "Tres son multitud" to "Trois, c'est trop",
    "3 días distintos despertando." to "Réveillez-vous 3 jours distincts.",
    "No hay semana sin siete días" to "Pas de semaine sans sept jours",
    "7 días distintos despertando." to "Réveillez-vous 7 jours distincts.",
    "Nuevo Hábito Adquirido" to "Nouvelle habitude acquise",
    "21 días distintos. Ya es rutina." to "21 jours distincts. C'est une routine désormais.",
    "Mes de Hierro" to "Mois de fer",
    "30 días distintos despertando." to "Réveillez-vous 30 jours distincts.",
    "Ambicioso" to "Ambitieux",
    "100 días distintos. Leyenda." to "100 jours distincts. Légende.",
    "2x2=?" to "2x2=?",
    "Completa 10 desafíos de matemáticas." to "Terminez 10 défis de calcul.",
    "Despacito y con buena letra" to "Doucement et proprement",
    "Completa 10 desafíos de escribir." to "Terminez 10 défis d'écriture.",
    "No te pases de la raya" to "Ne dépassez pas la ligne",
    "Completa 10 desafíos de delinear." to "Terminez 10 défis de tracé.",
    "Cuádruple o nada" to "Quadruple ou rien",
    "Supera una alarma de 4 rondas." to "Surmontez une alarme de 4 manches.",
    "Madrugador" to "Lève-tôt",
    "Levántate a las 7:00h o antes." to "Levez-vous à 7h00 ou plus tôt.",
    "Comprador" to "Acheteur",
    "Compra un objeto en la Tienda." to "Achetez un objet dans la Boutique.",
    "Coleccionista" to "Collectionneur",
    "Compra 10 objetos en la Tienda." to "Achetez 10 objets dans la Boutique.",
    "Avaricioso" to "Avare",
    "Acumula 5,000 puntos." to "Accumulez 5 000 points.",
    "Caja Tonta" to "Boîte à idioties",
    "Ve un anuncio recompensado." to "Regardez une publicité récompensée.",

    // ---------- Boutique ----------
    "Mejoras permanentes" to "Améliorations permanentes",
    "🧪 Pruebas: +50 🪙" to "🧪 Tests : +50 🪙",
    "MEJORAS INDIVIDUALES" to "AMÉLIORATIONS INDIVIDUELLES",
    "Alarmas Infinitas" to "Alarmes infinies",
    "Crea todas las alarmas que quieras sin límite." to "Créez autant d'alarmes que vous voulez, sans limite.",
    "Nuevos Marcadores" to "Nouveaux marqueurs",
    "Desbloquea 10 couleurs para organizar tus alarmas." to "Débloquez 10 couleurs pour organiser vos alarmes.",
    "Más Asaltos" to "Plus de manches",
    "Aumenta el limite de rondas por alarma a 10." to "Augmentez la limite de manches par alarme à 10.",
    "Elimina todos los anuncios" to "Supprimer toutes les publicités",
    "Una expérience totalement épurée et sans interruptions." to "Une expérience totalement épurée et sans interruptions.",
    "Desbloquea tu Avatar" to "Débloquez votre avatar",
    "Muestra una foto de perfil personalizada en la app." to "Affichez une photo de profil personnalisée dans l'application.",
    "Multiplicador de XP" to "Multiplicateur d'XP",
    "Gana el doble de experiencia al despertar." to "Gagnez le double d'expérience au réveil.",
    "TEMAS" to "THÈMES",
    "Cambia el color de toda la interfaz. Equípalo desde Ajustes." to "Change la couleur de toute l'interface. Équipez-le depuis les Réglages.",
    "Comprar" to "Acheter",
    "Comprado" to "Acheté",
    "Equipado" to "Équipé",

    // ---------- Thèmes ----------
    "Por defecto" to "Par défaut",
    "Aurum Legendario" to "Aurum légendaire",
    "Rosa Cyberpunk" to "Rose cyberpunk",
    "Verde Matrix" to "Vert Matrix",

    // ---------- Réglages ----------
    "Volumen de alarma" to "Volume de l'alarme",
    "Volumen del sistema" to "Volume du système",
    "Confirmar borrado" to "Confirmer la suppression",
    "Preguntar antes de eliminar una alarma." to "Demander avant de supprimer une alarme.",
    "PERSONALIZACIÓN" to "PERSONNALISATION",
    "Modo claro" to "Mode clair",
    "Interfaz clara activada." to "Interface claire activée.",
    "Interfaz oscura activada." to "Interface sombre activée.",
    "Sonido por defecto" to "Son par défaut",
    "Tono de alarma" to "Sonnerie d'alarme",
    "Tono de notificación" to "Son de notification",
    "Tono de llamada" to "Sonnerie d'appel",
    "Notificación" to "Notification",
    "Tema de la app" to "Thème de l'application",
    "Idioma" to "Langue",
    "Cómpralo en la Tienda" to "Achetez-le dans la Boutique",

    // ---------- Onboarding ----------
    "Selecciona tu idioma" to "Choisissez votre langue",
    "¿Cómo te llamas?" to "Comment vous appelez-vous ?",
    "Usaremos tu nombre para saludarte" to "Nous utiliserons votre nom pour vous saluer",
    "Tu nombre (opcional)" to "Votre nom (facultatif)",
    "¿Para qué usarás WakeApp?" to "Pourquoi utiliserez-vous WakeApp ?",
    "Elige tu objetivo principal" to "Choisissez votre objectif principal",
    "Productividad" to "Productivité",
    "Levantarme con energía para rendir más" to "Me lever avec énergie pour être plus productif",
    "Madrugar" to "Se lever tôt",
    "Quiero levantarme más temprano" to "Je veux me lever plus tôt",
    "Disciplina" to "Discipline",
    "Obligarme a despertarme sí o sí" to "M'obliger à me réveiller, sans excuse",
    "¿Qué dificultad prefieres?" to "Quelle difficulté préférez-vous ?",
    "Será la que uses por defecto al crear alarmas" to "Ce sera celle par défaut lors de la création d'alarmes",
    "Fácil" to "Facile",
    "Cálculos sencillos, frases cortas" to "Calculs simples, phrases courtes",
    "Media" to "Moyenne",
    "Cálculos medios, phrases normales" to "Calculs moyens, phrases normales",
    "Difícil" to "Difficile",
    "Multiplicaciones, phrases longues" to "Multiplications, phrases longues",
    "Extrema" to "Extrême",
    "Operaciones complejas, textos exactos" to "Opérations complexes, textes exacts",
    "¿A qué hora sueles despertarte?" to "À quelle heure vous levez-vous habituellement ?",
    "La usaremos como punto de partida al crear tus alarmas" to "Nous l'utiliserons comme point de départ lors de la création de vos alarmes",
    "Atrás" to "Retour",
    "Empezar" to "Commencer",

    // ---------- Écran du défi ----------
    "FRASE EXACTA" to "PHRASE EXACTE",
    "CÁLCULO MENTAL" to "CALCUL MENTAL",
    "Escribe la secuencia exacta que ves abajo:" to "Tapez la séquence exacte que vous voyez ci-dessous :",
    "Resuelve el cálculo para silenciar la alarma:" to "Résolvez le calcul pour couper l'alarme :",
    "Respuesta" to "Réponse",
    "Respuesta incorrecta. Inténtalo de nuevo." to "Réponse incorrecte. Réessayez.",
    "DESACTIVAR ALARMA" to "DÉSACTIVER L'ALARME",
    "DELINEADO PRECISO" to "TRACÉ PRÉCIS",
    "Traza la figura:" to "Tracez la figure :",
    "Círculo Neón" to "Cercle néon",
    "Triángulo" to "Triangle",
    "Estrella" to "Étoile",
    "Corazón" to "Cœur",
    "Onda Senoidal" to "Onde sinusoïdale",

    // ---------- Notification de l'alarme ----------
    "¡Alarma Sonando!" to "Alarme en cours !",
    "Resolver desafío" to "Résoudre le défi",
    "Toca para resolver el desafío" to "Touchez pour résoudre le défi",
    "Servicio de Alarma Activo" to "Service d'alarme actif",

    // ---------- Informations, Confidentialité et FAQ ----------
    "GENERAL" to "GÉNÉRAL",
    "Pedir confirmación antes de eliminar una alarma." to "Demander une confirmation avant de supprimer une alarme.",
    "Dificultad por defecto" to "Difficulté par défaut",
    "INFORMACIÓN DE LA APP" to "INFOS DE L'APPLI",
    "WakeApp es un despertador con desafíos interactivos diseñado para ayudarte a despertar de verdad, registrar tu avance y personalizar la app a tu gusto." to
            "WakeApp est un réveil avec des défis interactifs conçu pour vous aider à vraiment vous réveiller, suivre vos progrès et personnaliser l'application à votre goût.",
    "PRIVACIDAD DEL USUARIO" to "CONFIDENTIALITÉ UTILISATEUR",
    "Datos y preferencias" to "Données et préférences",
    "Tus alarmas, preferencias, progreso y niveles se almacenan en tu dispositivo. Si seleccionas un archivo de audio propio, la app solo utiliza el archivo que elijas." to
            "Vos alarmes, préférences, progrès et niveaux sont enregistrés sur votre appareil. Si vous sélectionnez votre propre fichier audio, l'application n'utilise que le fichier choisi.",
    "DUDAS FRECUENTES" to "FOIRE AUX QUESTIONS",
    "¿Cómo se desactiva una alarma?" to "Comment désactiver une alarme ?",
    "Al sonar la alarma tendrás que superar los desafíos configurados y las rondas indicadas para apagarla." to
            "Lorsque l'alarme sonne, vous devez réussir les défis configurés et le nombre de manches indiqué pour l'éteindre.",
    "¿Qué pasa si reinicio el móvil o cambia la hora?" to "Que se passe-t-il si je redémarre le téléphone ou si l'heure change ?",
    "Las alarmas activas se reprograman y automáticamente con el horario de tu dispositivo." to
            "Les alarmes actives sont reprogrammées automatiquement selon l'horaire de votre appareil.",
    "¿Cómo gano XP, niveles y puntos?" to "Comment gagner de l'XP, des niveaux et des points ?",
    "Completando desafíos sumas XP para subir de nivel, y al desbloquear logros obtienes puntos (🪙) para gastar en la Tienda." to
            "En réussissant des défis, vous gagnez de l'XP pour monter de niveau, et en débloquant des succès, vous obtenez des points (🪙) à dépenser dans la Boutique.",
    "¿Puedo usar mis propios archivos de audio?" to "Puis-je utiliser mes propres fichiers audio ?",
    "Sí, al configurar una alarma puedes elegir un archivo de audio desde tu dispositivo." to
            "Oui, lors de la configuration d'une alarme, vous pouvez choisir un fichier audio depuis votre appareil."
)