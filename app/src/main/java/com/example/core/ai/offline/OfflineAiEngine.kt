package com.example.core.ai.offline

import com.example.core.ai.AiProvider
import com.example.core.ai.AiResponse
import com.example.core.ai.DiagramLabel
import com.example.core.ai.ExtractedMemoryCandidate
import com.example.core.ai.FlashcardItem
import com.example.core.ai.FlowchartData
import com.example.core.ai.FlowchartNode
import com.example.core.ai.ProactiveFollowUp
import com.example.core.ai.QuizData
import com.example.core.ai.QuizQuestion
import com.example.core.ai.RevisionSummary
import com.example.core.ai.TeachAnalysisResult
import com.example.core.ai.TopicExplanation
import com.example.core.ai.Visual3DElement
import com.example.core.ai.Visual3DModel
import com.example.core.ai.VisualDiagramData
import com.example.data.local.entities.MemoryEntity
import com.example.data.local.entities.MessageEntity
import java.util.Calendar
import java.util.Locale

class OfflineAiEngine : AiProvider {

    override suspend fun generateCompanionResponse(
        userMessage: String,
        conversationHistory: List<MessageEntity>,
        activeMemories: List<MemoryEntity>,
        isStudyMode: Boolean,
        studyTopic: String?,
        language: String
    ): AiResponse {
        val lower = userMessage.trim().lowercase(Locale.ROOT)

        // Check for explicit memory commands
        if (lower.startsWith("remember that ") || lower.startsWith("remember: ") || lower.startsWith("remember ") || lower.contains("yaad rakhna")) {
            val content = userMessage.substringAfter("remember", "").substringAfter("yaad rakhna", "").trim().removePrefix("that ").removePrefix(":").trim()
            if (content.isNotBlank()) {
                val category = determineCategory(content)
                val reply = when (language) {
                    "HINDI" -> "मैंने यह याद रख लिया है: \"$content\"। आगे की बातचीत में मैं इसका ध्यान रखूँगी।"
                    "HINGLISH" -> "Main ne yeh note kar liya hai: \"$content\"! I'll always remember this, beta."
                    else -> "I've stored that in my memory: \"$content\". I'll keep this in mind."
                }
                return AiResponse(
                    text = reply,
                    extractedMemories = listOf(ExtractedMemoryCandidate(category, content)),
                    isDirectMemoryCommand = true,
                    memoryActionDescription = "Saved memory: $content"
                )
            }
        }

        if (lower.contains("what do you remember about me") || lower.contains("what do you remember") || lower == "memories" || lower.contains("kya yaad hai")) {
            if (activeMemories.isEmpty()) {
                val emptyReply = when (language) {
                    "HINDI" -> "अभी तक मेरे पास कोई विशेष यादें सहेजी नहीं गई हैं। आप मुझे बता सकते हैं जैसे 'याद रखना मुझे शाम को पढ़ना पसंद है'।"
                    "HINGLISH" -> "Abhi tak koi memory saved nahi hai. Aap mujhe bata sakte hain jaise 'Remember that I prefer visual explanations'!"
                    else -> "I don't have any specific memories saved yet. You can tell me things like 'Remember that I study best in the evening' or 'Remember I struggle with calculus'."
                }
                return AiResponse(text = emptyReply)
            }
            val memoryList = activeMemories.take(6).joinToString("\n• ") { it.content }
            val reply = when (language) {
                "HINDI" -> "मुझे आपके बारे में यह याद है:\n• $memoryList\n\nआप कभी भी मेमोरी टैब में जाकर इन्हें बदल सकते हैं।"
                "HINGLISH" -> "Here is what I remember about you:\n• $memoryList\n\nAap jab chahein isey edit ya delete kar sakte hain."
                else -> "Here is what I remember about you:\n• $memoryList\n\nYou can ask me to forget anything anytime in the Memory tab."
            }
            return AiResponse(text = reply)
        }

        // Check for automatic implicit memories
        val implicitMemories = extractImplicitMemories(userMessage)

        // Study context response
        if (isStudyMode && !studyTopic.isNullOrBlank()) {
            return AiResponse(
                text = generateStudyGuidance(userMessage, studyTopic, activeMemories, language),
                extractedMemories = implicitMemories,
                proactiveFollowUps = listOf(
                    ProactiveFollowUp("Explain with Flowchart & 3D", "EXPLAIN_FLOWCHART", studyTopic),
                    ProactiveFollowUp("Take a 3-Question Quiz", "QUIZ", studyTopic),
                    ProactiveFollowUp("Teach Mumma this Topic", "PRACTICE", studyTopic)
                )
            )
        }

        // Conversational companion responses adhering to Mumma persona:
        val reply = generatePersonaResponse(userMessage, activeMemories, conversationHistory, language)
        val followUps = generateSmartFollowUps(userMessage, studyTopic)
        return AiResponse(
            text = reply,
            extractedMemories = implicitMemories,
            proactiveFollowUps = followUps
        )
    }

    override suspend fun explainTopic(
        topic: String,
        language: String,
        relevantMemories: List<MemoryEntity>
    ): TopicExplanation {
        val topicLower = topic.lowercase(Locale.ROOT).trim()

        return when {
            topicLower.contains("photosynthesis") -> buildPhotosynthesisExplanation(language)
            topicLower.contains("atom") || topicLower.contains("bohr") || topicLower.contains("electron") -> buildAtomExplanation(language)
            topicLower.contains("dna") || topicLower.contains("genetics") || topicLower.contains("rna") -> buildDnaExplanation(language)
            topicLower.contains("mitosis") || topicLower.contains("cell") -> buildCellMitosisExplanation(language)
            topicLower.contains("newton") || topicLower.contains("gravity") || topicLower.contains("motion") -> buildNewtonExplanation(language)
            topicLower.contains("heart") || topicLower.contains("circulat") || topicLower.contains("blood") -> buildCirculatoryExplanation(language)
            else -> buildUniversalExplanation(topic, language)
        }
    }

    override suspend fun generateContextualGreeting(
        userName: String,
        language: String,
        activeMemories: List<MemoryEntity>,
        recentTopic: String
    ): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val timeGreeting = when {
            hour in 5..11 -> if (language == "HINDI") "शुभ प्रभात" else if (language == "HINGLISH") "Good morning beta" else "Good morning"
            hour in 12..16 -> if (language == "HINDI") "शुभ दोपहर" else if (language == "HINGLISH") "Good afternoon beta" else "Good afternoon"
            hour in 17..21 -> if (language == "HINDI") "शुभ संध्या" else if (language == "HINGLISH") "Good evening beta" else "Good evening"
            else -> if (language == "HINDI") "देर रात की पढ़ाई" else if (language == "HINGLISH") "Late night session" else "Late night study session"
        }

        val nameGreeting = if (userName.isNotBlank()) " $userName" else if (language != "ENGLISH") " beta" else " sweetheart"
        val struggle = activeMemories.find { it.category == "STRUGGLE" }?.content?.removePrefix("Struggles with ")
        val goal = activeMemories.find { it.category == "GOAL" }?.content?.removePrefix("Goal: ")

        return when (language) {
            "HINDI" -> when {
                goal != null -> "$timeGreeting$nameGreeting! आपके लक्ष्य '$goal' की दिशा में आज एक नया कदम उठाते हैं। क्या हम $recentTopic शुरू करें?"
                struggle != null -> "$timeGreeting$nameGreeting! घबराना बिल्कुल नहीं, $struggle को आज हम flowcharts और visual 3D से बिल्कुल आसान बना देंगे।"
                else -> "$timeGreeting$nameGreeting! मैं तुम्हारी मम्मा हूँ। बताओ आज क्या नया सीखना या समझना चाहते हो?"
            }
            "HINGLISH" -> when {
                goal != null -> "$timeGreeting$nameGreeting! Ready to make progress on '$goal'? Aaj $recentTopic ko flowcharts aur 3D visuals se master karte hain!"
                struggle != null -> "$timeGreeting$nameGreeting! Bilkul tension mat lo. $struggle ko hum step-by-step tod kar asaan banayenge."
                else -> "$timeGreeting$nameGreeting! Main Mumma hoon, your personal study companion. Aaj kya explore karein?"
            }
            else -> when {
                goal != null -> "$timeGreeting$nameGreeting! Ready to work toward '$goal'? Let's dive into $recentTopic with flowcharts, 3D models, and quizzes."
                struggle != null -> "$timeGreeting$nameGreeting! I'm right here with you. We'll conquer $struggle together step by step."
                else -> "$timeGreeting$nameGreeting! I'm Mumma, your personal study partner. What shall we learn, visualize, or revise together today?"
            }
        }
    }

    private fun generateSmartFollowUps(message: String, topic: String?): List<ProactiveFollowUp> {
        val lower = message.lowercase(Locale.ROOT)
        val activeTopic = topic ?: if (lower.contains("photosynthesis")) "Photosynthesis" else if (lower.contains("atom")) "Atomic Structure" else "Photosynthesis"

        return listOf(
            ProactiveFollowUp("Explain with Flowchart", "EXPLAIN_FLOWCHART", activeTopic),
            ProactiveFollowUp("View 3D Visualization", "OPEN_3D", activeTopic),
            ProactiveFollowUp("Take a 3-Question Quiz", "QUIZ", activeTopic)
        )
    }

    private fun buildPhotosynthesisExplanation(language: String): TopicExplanation {
        val englishText = """
            Photosynthesis is the fundamental biological engine that converts sunlight, water (H2O), and carbon dioxide (CO2) into chemical energy stored in glucose (C6H12O6), releasing life-giving oxygen (O2).
            
            It happens in two interconnected stages inside plant chloroplasts:
            1. Light-Dependent Reactions (Thylakoid Membrane):
               Sunlight photons strike chlorophyll pigments in Photosystems II and I. Water molecules are split (photolysis), freeing electrons, pumping protons, and producing ATP and NADPH, with Oxygen released as a byproduct.
            2. Light-Independent Reactions / Calvin Cycle (Stroma):
               The enzyme RuBisCO fixes atmospheric CO2 using the stored ATP and NADPH to synthesize high-energy sugars (G3P / Glucose).
        """.trimIndent()

        val hinglishText = "Photosynthesis ek aisa process hai jisme plants sunlight, paani aur carbon dioxide ka use karke glucose (food) aur oxygen banate hain. Thylakoid mein light energy convert hoti hai ATP aur NADPH mein, aur Stroma mein Calvin cycle se glucose banta hai."
        val hindiText = "प्रकाश संश्लेषण वह प्रक्रिया है जिसके द्वारा हरे पौधे सूर्य के प्रकाश, जल (H2O) और कार्बन डाइऑक्साइड (CO2) की मदद से ग्लूकोज बनाते हैं और ऑक्सीजन गैस छोड़ते हैं।"

        val nodes = listOf(
            FlowchartNode("1", 1, "Light Capture & Photolysis", "Photons hit Photosystem II in Thylakoids. H2O splits into O2 + protons + electrons.", "Light Reaction", "light_mode"),
            FlowchartNode("2", 2, "Electron Transport & ATP Synthesis", "Electrons travel across the thylakoid chain, creating a proton gradient that drives ATP Synthase.", "Energy Transfer", "bolt"),
            FlowchartNode("3", 3, "NADPH Generation", "Photosystem I energizes electrons to convert NADP+ into high-energy NADPH.", "Carrier", "battery_charging_full"),
            FlowchartNode("4", 4, "Carbon Fixation (RuBisCO)", "In the stroma, RuBisCO combines atmospheric CO2 with RuBP.", "Calvin Cycle", "autorenew"),
            FlowchartNode("5", 5, "Reduction & Glucose Output", "ATP and NADPH power the conversion into G3P sugar molecules, assembling glucose.", "Output", "eco")
        )

        val elements = listOf(
            Visual3DElement("Chloroplast Core", "Stroma Fluid", 30f, 0f, 0.4f, "#4CAF50"),
            Visual3DElement("Thylakoid Granum", "Light Phase", 18f, 55f, 1.2f, "#81C784"),
            Visual3DElement("Photon Wave (Sunlight)", "Excitation", 12f, 90f, 2.0f, "#FFF176"),
            Visual3DElement("Water (H2O)", "Electron Donor", 14f, 120f, 1.5f, "#4FC3F7"),
            Visual3DElement("RuBisCO Enzyme", "CO2 Fixer", 16f, 75f, 0.8f, "#FFB74D"),
            Visual3DElement("Glucose Molecule", "Energy Storage", 22f, 140f, 0.7f, "#E57373")
        )

        val labels = listOf(
            DiagramLabel("Chloroplast Outer Membrane", "Double membrane bounding the organelle", 0.2f, 0.2f),
            DiagramLabel("Thylakoid Lumen", "Site of light photolysis and ATP generation", 0.5f, 0.4f),
            DiagramLabel("Stroma", "Fluid filled matrix where Calvin cycle occurs", 0.75f, 0.65f),
            DiagramLabel("Granum Stacks", "Interconnected thylakoid discs", 0.35f, 0.75f)
        )

        return TopicExplanation(
            topic = "Photosynthesis",
            title = "Photosynthesis & Solar Energy Conversion",
            textExplanation = englishText,
            hinglishSummary = hinglishText,
            hindiSummary = hindiText,
            flowchart = FlowchartData("Photosynthesis", "Photosynthesis Step-by-Step Flow", nodes, "Converts 6CO2 + 6H2O + Sunlight → C6H12O6 + 6O2"),
            visual3D = Visual3DModel("CELL_ANATOMY", "Photosynthesis", "Chloroplast 3D Kinetic Model", "Interactive rotating 3D chloroplast organelle showing thylakoid grana, photon streams, and glucose synthesis pathways.", elements),
            diagram = VisualDiagramData("Photosynthesis", "Chloroplast Anatomy & Reaction Sites", "Detailed biological cross-section mapping light vs dark reaction compartments.", labels, visualType = "photosynthesis"),
            proactiveFollowUps = listOf(
                ProactiveFollowUp("Take a 3-Question Quiz", "QUIZ", "Photosynthesis"),
                ProactiveFollowUp("Explore 3D Chloroplast Model", "OPEN_3D", "Photosynthesis"),
                ProactiveFollowUp("Teach Mumma this Topic", "PRACTICE", "Photosynthesis")
            )
        )
    }

    private fun buildAtomExplanation(language: String): TopicExplanation {
        val englishText = """
            An Atom is the foundational unit of all chemical matter, consisting of a dense central nucleus surrounded by a cloud of orbiting electrons.
            
            Key Subatomic Constituents:
            1. Nucleus (Center): Contains positively charged Protons (determining the element's atomic number) and neutral Neutrons (providing nuclear strong force stability).
            2. Electron Shells (Orbits): Negatively charged electrons orbit in quantized energy levels (K, L, M shells) according to quantum principles.
            3. Chemical Reactivity: Determined primarily by valence electrons in the outermost shell seeking noble gas octet stability.
        """.trimIndent()

        val hinglishText = "Atom matter ki sabse basic unit hai. Beech mein nucleus hota hai jisme Protons (+) aur Neutrons hote hain, aur bahar electron shells mein Electrons (-) orbit karte hain."
        val hindiText = "परमाणु पदार्थ की मूल इकाई है। इसके केंद्र में एक नाभिक (न्यूक्लियस) होता है जिसमें प्रोटॉन और न्यूट्रॉन होते हैं, और इलेक्ट्रॉन विभिन्न कक्षाओं में परिक्रमा करते हैं।"

        val nodes = listOf(
            FlowchartNode("1", 1, "Dense Central Nucleus", "Protons (+ charge) and Neutrons bind tightly via the strong nuclear force.", "Core", "hub"),
            FlowchartNode("2", 2, "Inner K-Shell Orbit", "Hosts up to 2 electrons in the lowest, most tightly bound energy state.", "Ground Orbit", "radio_button_checked"),
            FlowchartNode("3", 3, "Outer Valence Shell", "Houses valence electrons that dictate chemical bonding, covalent sharing, or ionization.", "Valence", "share"),
            FlowchartNode("4", 4, "Energy Absorption / Emission", "Absorbing a photon promotes an electron to higher orbit; falling back emits characteristic light.", "Quantum Jump", "flare")
        )

        val elements = listOf(
            Visual3DElement("Nucleus (Protons+Neutrons)", "Dense Positive Core", 34f, 0f, 0.2f, "#E53935"),
            Visual3DElement("Electron 1 (K-Shell)", "Negative Charge", 12f, 50f, 2.5f, "#00E5FF", -15f),
            Visual3DElement("Electron 2 (K-Shell)", "Negative Charge", 12f, 50f, 2.5f, "#00E5FF", 15f),
            Visual3DElement("Valence Electron 3 (L-Shell)", "Chemical Bonding", 14f, 95f, 1.4f, "#76FF03", -30f),
            Visual3DElement("Valence Electron 4 (L-Shell)", "Chemical Bonding", 14f, 95f, 1.4f, "#76FF03", 30f),
            Visual3DElement("Valence Electron 5 (L-Shell)", "Valence", 14f, 135f, 0.9f, "#FFD600", 0f)
        )

        val labels = listOf(
            DiagramLabel("Central Nucleus", "Protons & Neutrons containing 99.9% of mass", 0.5f, 0.5f),
            DiagramLabel("K Shell Orbit", "n=1 Principal Quantum Number (Max 2 e-)", 0.3f, 0.3f),
            DiagramLabel("L Shell Orbit", "n=2 Principal Quantum Number (Max 8 e-)", 0.75f, 0.2f),
            DiagramLabel("Valence Electron Cloud", "Electrons available for chemical reactions", 0.8f, 0.75f)
        )

        return TopicExplanation(
            topic = "Atomic Structure",
            title = "Atomic Structure & Quantum Orbitals",
            textExplanation = englishText,
            hinglishSummary = hinglishText,
            hindiSummary = hindiText,
            flowchart = FlowchartData("Atomic Structure", "Atomic Organization & Energy Levels", nodes, "Atomic Number Z = Protons, Mass Number A = Protons + Neutrons"),
            visual3D = Visual3DModel("ATOM_ORBIT", "Atomic Structure", "Bohr-Rutherford 3D Orbital Model", "Interactive rotating 3D atom with orbiting electron clouds, nucleus pulsation, and energy shells.", elements),
            diagram = VisualDiagramData("Atomic Structure", "Subatomic Particles & Energy Orbitals", "Cross section diagram of nuclear binding and quantized orbital levels.", labels, visualType = "atom"),
            proactiveFollowUps = listOf(
                ProactiveFollowUp("Inspect 3D Orbital Model", "OPEN_3D", "Atomic Structure"),
                ProactiveFollowUp("Take a 3-Question Quiz", "QUIZ", "Atomic Structure"),
                ProactiveFollowUp("Teach Mumma this Topic", "PRACTICE", "Atomic Structure")
            )
        )
    }

    private fun buildDnaExplanation(language: String): TopicExplanation {
        val englishText = """
            Deoxyribonucleic Acid (DNA) is the biological blueprint of all living organisms, encoding genetic instructions in a double helix of nucleotide base pairs.
            
            Structure & Base Pairing Rules:
            - Backbone: Alternating deoxyribose sugar and phosphate groups linked by phosphodiester bonds.
            - Nitrogenous Bases: Adenine (A) pairs with Thymine (T) via 2 hydrogen bonds; Guanine (G) pairs with Cytosine (C) via 3 hydrogen bonds.
            - Directionality: Antiparallel 5' to 3' and 3' to 5' strands.
        """.trimIndent()

        val hinglishText = "DNA humare body ka genetic blueprint hai. Yeh ek double helix structure hota hai jisme Sugar-Phosphate backbone hoti hai aur 4 bases: A pairs with T, aur G pairs with C."
        val hindiText = "डीएनए (DNA) सभी जीवित जीवों का आनुवंशिक खाका है। यह एक द्विकुंडलिनी (Double Helix) संरचना है जिसमें चार क्षार होते हैं: एडेनिन, थायमिन, ग्वानिन और साइटोसिन।"

        val nodes = listOf(
            FlowchartNode("1", 1, "Sugar-Phosphate Backbone", "Deoxyribose sugars joined by phosphate bridges create structural spirals.", "Backbone", "linear_scale"),
            FlowchartNode("2", 2, "Hydrogen Base Pairing", "A=T (2 H-bonds) and G≡C (3 H-bonds) maintain perfect complementary spacing.", "Bonding", "link"),
            FlowchartNode("3", 3, "Transcription into mRNA", "RNA Polymerase reads the template strand to generate messenger RNA transcripts.", "Expression", "receipt_long"),
            FlowchartNode("4", 4, "Translation at Ribosome", "tRNA codons translate the nucleotide sequence into functional polypeptide protein chains.", "Synthesis", "precision_manufacturing")
        )

        val elements = listOf(
            Visual3DElement("Helix Strand 1 (5'→3')", "Sugar Backbone", 20f, 60f, 1.0f, "#29B6F6", -40f),
            Visual3DElement("Helix Strand 2 (3'→5')", "Antiparallel", 20f, 60f, 1.0f, "#AB47BC", 40f),
            Visual3DElement("Adenine-Thymine Pair", "2 H-Bonds", 15f, 30f, 1.0f, "#FFA726", -15f),
            Visual3DElement("Guanine-Cytosine Pair", "3 H-Bonds", 15f, 30f, 1.0f, "#66BB6A", 15f),
            Visual3DElement("Phosphate Node", "Negative Charge", 12f, 85f, 1.2f, "#EF5350", 0f)
        )

        val labels = listOf(
            DiagramLabel("Major Groove", "Wider helical gap where transcription factors bind", 0.3f, 0.25f),
            DiagramLabel("Minor Groove", "Narrower helical spiral", 0.7f, 0.45f),
            DiagramLabel("Base Pair rungs", "Complementary hydrogen-bonded nucleotides", 0.5f, 0.6f),
            DiagramLabel("5' to 3' Antiparallel Strand", "Phosphodiester backbone orientation", 0.2f, 0.8f)
        )

        return TopicExplanation(
            topic = "DNA Structure",
            title = "DNA Double Helix & Molecular Genetics",
            textExplanation = englishText,
            hinglishSummary = hinglishText,
            hindiSummary = hindiText,
            flowchart = FlowchartData("DNA Structure", "Genetic Code & Protein Synthesis Flow", nodes, "Central Dogma: DNA → Transcription → mRNA → Translation → Protein"),
            visual3D = Visual3DModel("DNA_HELIX", "DNA Structure", "DNA Double Helix 3D Kinetic Model", "Interactive rotating 3D helical staircase showing base pairing (A-T, G-C) and phosphate backbones.", elements),
            diagram = VisualDiagramData("DNA Structure", "Nucleotide Chemistry & Helical Grooves", "Molecular diagram detailing hydrogen bonds and antiparallel helical symmetry.", labels, visualType = "generic"),
            proactiveFollowUps = listOf(
                ProactiveFollowUp("Inspect 3D Double Helix", "OPEN_3D", "DNA Structure"),
                ProactiveFollowUp("Take a 3-Question Quiz", "QUIZ", "DNA Structure"),
                ProactiveFollowUp("Teach Mumma this Topic", "PRACTICE", "DNA Structure")
            )
        )
    }

    private fun buildCellMitosisExplanation(language: String): TopicExplanation {
        val englishText = """
            Mitosis is the precise process of somatic cell division wherein a single parent cell replicates its genome and divides into two genetically identical daughter cells.
            
            The Four Core Stages (PMAT):
            1. Prophase: Chromatin condenses into visible chromosomes; mitotic spindle forms; nuclear envelope breaks down.
            2. Metaphase: Chromosomes align along the metaphase plate (cell equator) attached to spindle fibers at their kinetochores.
            3. Anaphase: Sister chromatids are pulled apart toward opposite cellular poles by shortening microtubules.
            4. Telophase & Cytokinesis: Nuclear membranes reform, chromosomes decondense, and the cytoplasm divides.
        """.trimIndent()

        val hinglishText = "Mitosis cell division ka process hai jisme ek single cell do identical daughter cells mein divide hota hai. Stages: Prophase, Metaphase, Anaphase aur Telophase (PMAT)."
        val hindiText = "समसूत्री विभाजन (Mitosis) वह प्रक्रिया है जिसमें एक जनक कोशिका विभाजित होकर दो समान संतति कोशिकाओं का निर्माण करती है।"

        val nodes = listOf(
            FlowchartNode("1", 1, "Interphase (G1, S, G2)", "Cell grows, replicates DNA and centrosomes in preparation for division.", "Prep", "hourglass_empty"),
            FlowchartNode("2", 2, "Prophase", "Chromosomes condense into distinct X-shapes; nuclear envelope dissolves.", "Stage 1", "filter_center_focus"),
            FlowchartNode("3", 3, "Metaphase", "Spindle fibers align all chromosomes strictly along the cell equator.", "Stage 2", "view_column"),
            FlowchartNode("4", 4, "Anaphase", "Kinetochore microtubules pull sister chromatids apart to opposite poles.", "Stage 3", "call_split"),
            FlowchartNode("5", 5, "Telophase & Cytokinesis", "Cleavage furrow pinches cell into two genetically identical diploid cells.", "Outcome", "people")
        )

        val elements = listOf(
            Visual3DElement("Centrosome Aster Pole A", "Spindle Organizer", 22f, 80f, 0.8f, "#AB47BC", -50f),
            Visual3DElement("Centrosome Aster Pole B", "Spindle Organizer", 22f, 80f, 0.8f, "#AB47BC", 50f),
            Visual3DElement("Chromosome Set (Equator)", "Genetic Material", 30f, 0f, 0.5f, "#EC407A", 0f),
            Visual3DElement("Kinetochore Spindle Fibers", "Microtubules", 14f, 45f, 1.5f, "#26C6DA", -20f),
            Visual3DElement("Cleavage Ring", "Actin Filament", 16f, 110f, 0.6f, "#FFA726", 0f)
        )

        return TopicExplanation(
            topic = "Cell Mitosis",
            title = "Cell Mitosis & Somatic Division",
            textExplanation = englishText,
            hinglishSummary = hinglishText,
            hindiSummary = hindiText,
            flowchart = FlowchartData("Cell Mitosis", "Mitosis PMAT Lifecycle Flow", nodes, "1 Parent Cell (2n) → 2 Identical Daughter Cells (2n)"),
            visual3D = Visual3DModel("CELL_ANATOMY", "Cell Mitosis", "Mitotic Spindle 3D Interactive Model", "Interactive rotating 3D cell showing centrosomes, spindle fibers, and chromosome separation dynamics.", elements),
            diagram = VisualDiagramData("Cell Mitosis", "Stages of Mitosis (PMAT)", "Visual comparison of prophase, metaphase, anaphase, and cytokinesis.", emptyList(), visualType = "cell"),
            proactiveFollowUps = listOf(
                ProactiveFollowUp("Inspect 3D Mitotic Spindle", "OPEN_3D", "Cell Mitosis"),
                ProactiveFollowUp("Take a 3-Question Quiz", "QUIZ", "Cell Mitosis"),
                ProactiveFollowUp("Teach Mumma this Topic", "PRACTICE", "Cell Mitosis")
            )
        )
    }

    private fun buildNewtonExplanation(language: String): TopicExplanation {
        val englishText = """
            Newton's Laws of Motion formulate the bedrock of classical mechanics, describing how physical forces govern the motion of objects:
            
            1. First Law (Inertia): An object remains at rest or in uniform motion along a straight line unless acted upon by a non-zero net external force.
            2. Second Law (F = ma): Acceleration is directly proportional to net force and inversely proportional to mass (Vector equation: F_net = m * a).
            3. Third Law (Action-Reaction): For every action force exerted on body B by body A, body B exerts an equal and opposite reaction force on body A.
        """.trimIndent()

        val hinglishText = "Newton ke 3 laws of motion physics ka base hain: 1st Law (Inertia - bina force ke motion change nahi hota), 2nd Law (F = m * a), aur 3rd Law (Har action ka equal aur opposite reaction hota hai)."
        val hindiText = "न्यूटन के गति के नियम: प्रथम नियम (जड़त्व का नियम), द्वितीय नियम (बल = द्रव्यमान × त्वरण, F = ma), और तृतीय नियम (क्रिया-प्रतिक्रिया का नियम)।"

        val nodes = listOf(
            FlowchartNode("1", 1, "Identify System & Mass (m)", "Isolate the object of interest and determine its inertial mass in kilograms.", "Setup", "scale"),
            FlowchartNode("2", 2, "Draw Free Body Diagram", "Resolve all vector forces (Gravity, Normal, Friction, Tension, Applied).", "Forces", "alt_route"),
            FlowchartNode("3", 3, "Compute Net Force (ΣF)", "Sum vector components along X and Y axes (ΣFx = m*ax, ΣFy = m*ay).", "Calculation", "calculate"),
            FlowchartNode("4", 4, "Determine Acceleration (a)", "Use a = ΣF / m to predict kinematic trajectory and velocity over time.", "Kinematics", "speed")
        )

        val elements = listOf(
            Visual3DElement("Mass Block (m)", "Inertial Mass", 32f, 0f, 0.3f, "#FFA726"),
            Visual3DElement("Applied Force Vector (F)", "Vector Magnitude", 16f, 70f, 1.2f, "#42A5F5", -25f),
            Visual3DElement("Frictional Opposing Vector", "Resistance", 14f, 50f, 1.2f, "#EF5350", 25f),
            Visual3DElement("Gravitational Force (mg)", "Downward Vector", 16f, 90f, 0.8f, "#7E57C2", -60f),
            Visual3DElement("Normal Reaction (N)", "Upward Contact", 16f, 90f, 0.8f, "#66BB6A", 60f)
        )

        return TopicExplanation(
            topic = "Newton's Laws",
            title = "Newton's Laws & Force Vectors",
            textExplanation = englishText,
            hinglishSummary = hinglishText,
            hindiSummary = hindiText,
            flowchart = FlowchartData("Newton's Laws", "Free-Body Force Analysis Protocol", nodes, "ΣF = m * a | Momentum p = m * v | Impulse J = Δp"),
            visual3D = Visual3DModel("PHYSICS_VECTORS", "Newton's Laws", "Force Vectors 3D Interactive Model", "Interactive rotating 3D physics coordinate space with live vector arrows and mass equilibrium.", elements),
            diagram = VisualDiagramData("Newton's Laws", "Free Body Diagram & Vector Resolution", "Vector breakdown of incline planes, friction, and normal forces.", emptyList(), visualType = "generic"),
            proactiveFollowUps = listOf(
                ProactiveFollowUp("Inspect 3D Force Vectors", "OPEN_3D", "Newton's Laws"),
                ProactiveFollowUp("Take a 3-Question Quiz", "QUIZ", "Newton's Laws"),
                ProactiveFollowUp("Teach Mumma this Topic", "PRACTICE", "Newton's Laws")
            )
        )
    }

    private fun buildCirculatoryExplanation(language: String): TopicExplanation {
        val englishText = """
            The Human Cardiovascular System is a continuous closed-loop transport network driven by the muscular 4-chambered heart.
            
            Double Circulation Pathway:
            1. Pulmonary Circulation: Deoxygenated blood enters the Right Atrium → Right Ventricle → Pulmonary Arteries → Lungs (oxygenation and CO2 discharge) → Pulmonary Veins → Left Atrium.
            2. Systemic Circulation: Oxygen-rich blood pumps from Left Ventricle → Aorta → Systemic Arteries → Capillary bed tissues (nutrient exchange) → Vena Cava → Right Atrium.
        """.trimIndent()

        val hinglishText = "Cardiovascular system heart aur blood vessels ka network hai. Heart ke 4 chambers hote hain: Right side deoxygenated blood ko lungs bhejti hai, aur Left side oxygenated blood ko poori body mein pump karti hai."
        val hindiText = "मानव परिसंचरण तंत्र (Circulatory System) हृदय और रक्त वाहिकाओं का जाल है। हृदय के 4 कक्ष होते हैं जो फेफड़ों और पूरे शरीर में रक्त का निरंतर प्रवाह बनाए रखते हैं।"

        val nodes = listOf(
            FlowchartNode("1", 1, "Right Atrium & Ventricle", "Receives deoxygenated venous return from Superior & Inferior Vena Cava.", "Deoxygenated", "water_drop"),
            FlowchartNode("2", 2, "Pulmonary Loop to Lungs", "Pulmonary artery delivers blood to lung alveoli for O2 uptake and CO2 release.", "Oxygenation", "air"),
            FlowchartNode("3", 3, "Left Atrium & Left Ventricle", "Oxygenated blood returns via pulmonary veins to the powerful left ventricle.", "High Pressure", "favorite"),
            FlowchartNode("4", 4, "Aorta & Systemic Delivery", "High-pressure arterial distribution to cerebral, coronary, and peripheral tissues.", "Perfusion", "local_shipping")
        )

        val elements = listOf(
            Visual3DElement("Heart Muscular Core", "4-Chamber Pump", 36f, 0f, 0.4f, "#E53935"),
            Visual3DElement("Aorta High-Pressure Arch", "Systemic Arteries", 18f, 65f, 1.4f, "#FF5252", -30f),
            Visual3DElement("Vena Cava Return Loop", "Venous Deox", 18f, 65f, 1.4f, "#1E88E5", 30f),
            Visual3DElement("Pulmonary Capillary Bed", "Gas Exchange", 14f, 105f, 1.1f, "#AB47BC", 0f),
            Visual3DElement("Oxygenated RBC Stream", "Hemoglobin", 12f, 130f, 1.8f, "#FF1744", 0f)
        )

        return TopicExplanation(
            topic = "Circulatory System",
            title = "Circulatory System & Double Circulation",
            textExplanation = englishText,
            hinglishSummary = hinglishText,
            hindiSummary = hindiText,
            flowchart = FlowchartData("Circulatory System", "Blood Perfusion & Double Circulation Flow", nodes, "Pulmonary Loop (Lungs) + Systemic Loop (Body Organs)"),
            visual3D = Visual3DModel("CELL_ANATOMY", "Circulatory System", "Cardiovascular 3D Chamber Model", "Interactive rotating 3D heart with animated oxygenated (red) and deoxygenated (blue) flow loops.", elements),
            diagram = VisualDiagramData("Circulatory System", "Heart Chambers & Major Vessels", "Detailed 4-chamber anatomical flow mapping tricuspid, bicuspid, and semilunar valves.", emptyList(), visualType = "circulatory"),
            proactiveFollowUps = listOf(
                ProactiveFollowUp("Inspect 3D Cardiovascular Model", "OPEN_3D", "Circulatory System"),
                ProactiveFollowUp("Take a 3-Question Quiz", "QUIZ", "Circulatory System"),
                ProactiveFollowUp("Teach Mumma this Topic", "PRACTICE", "Circulatory System")
            )
        )
    }

    private fun buildUniversalExplanation(topic: String, language: String): TopicExplanation {
        val englishText = """
            $topic represents a foundational subject in science and analytical inquiry.
            
            Core Conceptual Framework:
            1. First Principles & Definition: What $topic fundamentally is, why it occurs, and the core laws governing its behavior.
            2. System Transformation: How initial inputs or states undergo reactions, forces, or transformations to produce definitive outputs.
            3. Real-World Applications: Practical problem-solving, technological innovations, and analytical problem models.
        """.trimIndent()

        val hinglishText = "$topic ko samajhne ke liye pehle iske basic principles samjho, fir step-by-step process dekho ki kaise inputs se outputs bante hain."
        val hindiText = "$topic को गहराई से समझने के लिए इसके मूल सिद्धांतों और चरणबद्ध प्रक्रियाओं का अध्ययन आवश्यक है।"

        val nodes = listOf(
            FlowchartNode("1", 1, "Fundamental Premises", "Define the baseline inputs, variables, and boundary conditions for $topic.", "Input", "input"),
            FlowchartNode("2", 2, "Core Interaction / Transformation", "The primary operating mechanism, mathematical law, or biological reaction.", "Process", "autorenew"),
            FlowchartNode("3", 3, "Equilibrium & Output", "The resulting product, energy state, or observable measurable outcome.", "Output", "check_circle")
        )

        val elements = listOf(
            Visual3DElement("Core Entity", "$topic Center", 30f, 0f, 0.4f, "#FFB74D"),
            Visual3DElement("Influencing Node A", "Variable A", 15f, 60f, 1.2f, "#4FC3F7", -20f),
            Visual3DElement("Influencing Node B", "Variable B", 15f, 60f, 1.2f, "#81C784", 20f),
            Visual3DElement("Resulting State", "Outcome", 18f, 110f, 0.8f, "#E57373", 0f)
        )

        return TopicExplanation(
            topic = topic,
            title = "$topic Comprehensive Breakdown",
            textExplanation = englishText,
            hinglishSummary = hinglishText,
            hindiSummary = hindiText,
            flowchart = FlowchartData(topic, "$topic Step-by-Step Flowchart", nodes, "Step-by-step operational framework for $topic"),
            visual3D = Visual3DModel("ATOM_ORBIT", topic, "$topic 3D Interactive Model", "Interactive rotating 3D conceptual space for $topic.", elements),
            diagram = VisualDiagramData(topic, "$topic Schematic Diagram", "Schematic diagram for $topic", emptyList(), visualType = "generic"),
            proactiveFollowUps = listOf(
                ProactiveFollowUp("Take a 3-Question Quiz", "QUIZ", topic),
                ProactiveFollowUp("Explore 3D Visualization", "OPEN_3D", topic),
                ProactiveFollowUp("Teach Mumma this Topic", "PRACTICE", topic)
            )
        )
    }

    override suspend fun analyzeTeachExplanation(
        topic: String,
        userExplanation: String,
        relevantMemories: List<MemoryEntity>,
        language: String
    ): TeachAnalysisResult {
        val lower = userExplanation.lowercase(Locale.ROOT)
        val wordCount = userExplanation.split("\\s+".toRegex()).size

        val topicLower = topic.lowercase(Locale.ROOT)
        val knownConcepts = getTopicConcepts(topicLower)

        val mentioned = mutableListOf<String>()
        val missed = mutableListOf<String>()

        for (concept in knownConcepts.keyConcepts) {
            if (concept.aliases.any { lower.contains(it.lowercase(Locale.ROOT)) }) {
                mentioned.add(concept.name)
            } else {
                missed.add(concept.name)
            }
        }

        val corrections = mutableListOf<String>()
        for (pitfall in knownConcepts.commonErrors) {
            if (pitfall.triggerKeywords.all { lower.contains(it.lowercase(Locale.ROOT)) }) {
                corrections.add(pitfall.correction)
            }
        }

        val clarityRating = when {
            wordCount > 60 && mentioned.size >= 2 -> if (language == "HINDI") "स्पष्ट और सुव्यवस्थित" else "Clear and well-structured"
            wordCount > 30 -> if (language == "HINDI") "अच्छा आधार, तार्किक क्रम" else "Good foundation, logical progression"
            wordCount > 15 -> if (language == "HINDI") "संक्षिप्त, थोड़ा और विस्तार करें" else "Concise, but could use more depth"
            else -> if (language == "HINDI") "बहुत संक्षिप्त; मुख्य बिंदुओं को शामिल करें" else "Brief explanation; expand on the underlying mechanisms"
        }

        val scoreRatio = if (knownConcepts.keyConcepts.isNotEmpty()) {
            (mentioned.size.toFloat() / knownConcepts.keyConcepts.size.toFloat())
        } else {
            (wordCount.coerceIn(10, 100) / 100f)
        }

        val lengthFactor = (wordCount / 50f).coerceIn(0.2f, 1.0f)
        val penaltyFactor = (1.0f - (corrections.size * 0.25f)).coerceIn(0.3f, 1.0f)
        val calculatedConfidence = ((scoreRatio * 60 + lengthFactor * 40) * penaltyFactor).toInt().coerceIn(35, 96)

        val understandingText = if (mentioned.isNotEmpty()) {
            when (language) {
                "HINDI" -> "आपने इन मुख्य बिंदुओं को बहुत अच्छे से समझाया: ${mentioned.joinToString(", ")}।"
                "HINGLISH" -> "Aapne yeh concepts bohot achhe se cover kiye: ${mentioned.joinToString(", ")}. Solid clarity!"
                else -> "You clearly grasped: ${mentioned.joinToString(", ")}. Good explanation on how these connect."
            }
        } else if (wordCount > 20) {
            when (language) {
                "HINDI" -> "आपने $topic के बुनियादी सिद्धांतों की अच्छी शुरुआत की।"
                "HINGLISH" -> "Aapne $topic ki acchi conceptual shuruat ki hai."
                else -> "You set up a solid conceptual introduction to $topic with good intuition."
            }
        } else {
            "You introduced the foundational premise of $topic."
        }

        val followUps = mutableListOf<String>()
        if (missed.isNotEmpty()) {
            followUps.add("How does ${missed.first()} play into this process?")
        }
        if (missed.size > 1) {
            followUps.add("Can you explain the exact mechanism behind ${missed[1]}?")
        }
        if (followUps.isEmpty()) {
            followUps.add("What happens if this system undergoes extreme conditions or disruption?")
            followUps.add("How would you summarize the end-to-end outcome in one sentence?")
        }

        val summaryVoice = when {
            corrections.isNotEmpty() -> when (language) {
                "HINDI" -> "आपने अच्छा समझाया, लेकिन एक बात ध्यान रखें: ${corrections.first()}।"
                "HINGLISH" -> "Overall accha attempt tha beta! Bas yeh dhyaan rakhna: ${corrections.first()}."
                else -> "You have a good grasp of the big picture, but watch out: ${corrections.first()}. Let's refine that."
            }
            missed.isNotEmpty() -> when (language) {
                "HINDI" -> "बहुत बढ़िया प्रयास! आपने मुख्य बातें बताईं, लेकिन ${missed.take(2).joinToString(" और ")} का भी ज़िक्र करें।"
                "HINGLISH" -> "Shabash beta! You covered ${if (mentioned.isNotEmpty()) mentioned.first() else "the basics"}, par ${missed.take(2).joinToString(" and ")} ko bhi add karna mat bhoolna."
                else -> "Good start! You covered ${if (mentioned.isNotEmpty()) mentioned.first() else "the basics"}, but didn't mention ${missed.take(2).joinToString(" and ")}. Let's work through those."
            }
            else -> when (language) {
                "HINDI" -> "लाजवाब! आपने सभी मुख्य घटकों को बहुत स्पष्ट और तार्किक रूप से समझाया।"
                "HINGLISH" -> "Superb explanation beta! Sabhi points clear aur structured hain."
                else -> "Solid explanation. You covered the main components logically. Now try answering the follow-up questions."
            }
        }

        return TeachAnalysisResult(
            understanding = understandingText,
            missing = if (missed.isEmpty()) listOf("No major concept gaps in this summary.") else missed,
            corrections = if (corrections.isEmpty()) listOf("No major factual errors detected.") else corrections,
            clarity = clarityRating,
            confidenceScore = calculatedConfidence,
            followUpQuestions = followUps.take(3),
            summaryMessage = summaryVoice
        )
    }

    override suspend fun generateQuiz(
        topic: String,
        relevantMemories: List<MemoryEntity>,
        language: String
    ): QuizData {
        val topicLower = topic.lowercase(Locale.ROOT)
        val prebuilt = getPrebuiltQuiz(topicLower)
        if (prebuilt != null) return prebuilt

        val q1 = QuizQuestion(
            question = "What is the primary governing principle or goal of $topic?",
            options = listOf(
                "Facilitating core system transformation or state change",
                "Maintaining static equilibrium without energetic exchange",
                "Arbitrary reaction without causal inputs",
                "Completely eliminating entropy permanently"
            ),
            correctIndex = 0,
            explanation = "In $topic, the fundamental purpose centers on driving state changes and energy/information transfer."
        )

        val q2 = QuizQuestion(
            question = "Which factor is essential when analyzing or applying $topic?",
            options = listOf(
                "Ignoring environmental boundary conditions",
                "Understanding the relationship between components and constraints",
                "Assuming zero variables or external influences",
                "Treating all variables as constant indefinitely"
            ),
            correctIndex = 1,
            explanation = "Analyzing $topic requires identifying the interaction between key components and their operational constraints."
        )

        val q3 = QuizQuestion(
            question = "What is a common pitfall students encounter when studying $topic?",
            options = listOf(
                "Focusing on the first-principles mechanism",
                "Memorizing formulas or terms without conceptual intuition",
                "Drawing diagrams to visualize stages",
                "Explaining the concepts orally"
            ),
            correctIndex = 1,
            explanation = "A frequent obstacle in $topic is rote memorization rather than developing intuition for how parts interact."
        )

        return QuizData(
            topic = topic,
            questions = listOf(q1, q2, q3)
        )
    }

    override suspend fun generateFlashcards(topic: String, language: String): List<FlashcardItem> {
        val topicLower = topic.lowercase(Locale.ROOT)
        val prebuilt = getPrebuiltFlashcards(topicLower)
        if (prebuilt.isNotEmpty()) return prebuilt

        return listOf(
            FlashcardItem(
                keyTerm = "$topic: Core Definition",
                front = "What is the primary definition and significance of $topic?",
                back = "$topic represents the core phenomenon/system responsible for dynamic processes and structure in its field."
            ),
            FlashcardItem(
                keyTerm = "$topic: Key Mechanism",
                front = "What is the step-by-step mechanism that drives $topic?",
                back = "It begins with initial conditions/inputs, proceeds through transformation cycles, and produces distinct measurable outputs."
            ),
            FlashcardItem(
                keyTerm = "$topic: Practical Application",
                front = "Where is $topic encountered in real-world scenarios or problem solving?",
                back = "Used across applied sciences and analytical problem solving to predict behaviors and optimize outcomes."
            )
        )
    }

    override suspend fun generateQuickRevision(
        topic: String,
        struggles: List<String>,
        language: String
    ): RevisionSummary {
        val topicLower = topic.lowercase(Locale.ROOT)
        val prebuilt = getPrebuiltRevision(topicLower)
        if (prebuilt != null) return prebuilt

        return RevisionSummary(
            topic = topic,
            coreDefinition = "$topic: Fundamental concept centering on systematic rules, energy/information flows, and observable outcomes.",
            keyPoints = listOf(
                "Always map out inputs, transformations, and end products.",
                "Understand why each step happens rather than just the sequence.",
                "Identify boundary constraints and controlling variables."
            ),
            commonPitfalls = listOf(
                "Confusing correlation with causal mechanisms.",
                "Skipping over intermediate reaction/processing stages."
            ),
            mummaStudyTip = "Try closing your notes right now and explaining $topic out loud in three sentences."
        )
    }

    override suspend fun extractMemories(userMessage: String): List<ExtractedMemoryCandidate> {
        return extractImplicitMemories(userMessage)
    }

    private fun determineCategory(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("prefer") || lower.contains("like") || lower.contains("favorite") || lower.contains("hate") -> "PREFERENCE"
            lower.contains("struggle") || lower.contains("hard") || lower.contains("difficult") || lower.contains("confused") -> "STRUGGLE"
            lower.contains("master") || lower.contains("good at") || lower.contains("expert") -> "MASTERY"
            lower.contains("goal") || lower.contains("aim") || lower.contains("target") || lower.contains("exam") -> "GOAL"
            lower.contains("always") || lower.contains("never") || lower.contains("instruction") || lower.contains("remind") -> "INSTRUCTION"
            else -> "CONTEXT"
        }
    }

    private fun extractImplicitMemories(text: String): List<ExtractedMemoryCandidate> {
        val candidates = mutableListOf<ExtractedMemoryCandidate>()
        val lower = text.lowercase(Locale.ROOT)

        if (lower.contains("i struggle with ") || lower.contains("i find it hard to ")) {
            val struggle = text.substringAfter("struggle with", "").substringAfter("hard to", "").take(80).trim().removeSuffix(".")
            if (struggle.isNotBlank()) {
                candidates.add(ExtractedMemoryCandidate("STRUGGLE", "Struggles with $struggle"))
            }
        }

        if (lower.contains("i prefer ") || lower.contains("i like studying ")) {
            val pref = text.substringAfter("prefer", "").substringAfter("like studying", "").take(80).trim().removeSuffix(".")
            if (pref.isNotBlank()) {
                candidates.add(ExtractedMemoryCandidate("PREFERENCE", "Prefers $pref"))
            }
        }

        if (lower.contains("my goal is ") || lower.contains("i want to achieve ") || lower.contains("preparing for ")) {
            val goal = text.substringAfter("goal is", "").substringAfter("achieve", "").substringAfter("preparing for", "").take(80).trim().removeSuffix(".")
            if (goal.isNotBlank()) {
                candidates.add(ExtractedMemoryCandidate("GOAL", "Goal: $goal"))
            }
        }

        return candidates
    }

    private fun generatePersonaResponse(
        message: String,
        activeMemories: List<MemoryEntity>,
        history: List<MessageEntity>,
        language: String
    ): String {
        val lower = message.trim().lowercase(Locale.ROOT)

        // Check greeting
        if (lower.matches(Regex("^(hi|hello|hey|good morning|good afternoon|good evening|mumma|hey mumma|yo|howdy|namaste|pranam).*"))) {
            val struggleMemory = activeMemories.find { it.category == "STRUGGLE" }
            val goalMemory = activeMemories.find { it.category == "GOAL" }

            return when (language) {
                "HINDI" -> when {
                    goalMemory != null -> "नमस्ते बेटा! कैसे हो? आपके लक्ष्य '${goalMemory.content.removePrefix("Goal: ")}' पर कैसी प्रगति चल रही है?"
                    struggleMemory != null -> "नमस्ते बेटा! मैं यहीं तुम्हारे साथ हूँ। क्या आज ${struggleMemory.content.removePrefix("Struggles with ")} को आसान बनाएँ?"
                    else -> "नमस्ते बेटा! मैं तुम्हारी मम्मा हूँ। बताओ आज क्या पढ़ना या बात करना चाहते हो?"
                }
                "HINGLISH" -> when {
                    goalMemory != null -> "Hey beta! Good to see you. How is progress on ${goalMemory.content.removePrefix("Goal: ")}? Aaj kya conquer karein?"
                    struggleMemory != null -> "Hey beta! Main yahin hoon. Still working on ${struggleMemory.content.removePrefix("Struggles with ")}, ya aaj koi fresh topic lein?"
                    else -> "Hey beta! Main ready hoon. What's on your mind today—chat, study, flowchart, ya revision?"
                }
                else -> when {
                    goalMemory != null -> "Hey there! Good to see you. How is progress on ${goalMemory.content.removePrefix("Goal: ")}? What are we working on right now?"
                    struggleMemory != null -> "Hey! I'm right here with you. Still working through ${struggleMemory.content.removePrefix("Struggles with ")}, or should we tackle a fresh topic today?"
                    else -> "Hey there! I'm here and ready to roll. What's on your mind today—chat, study, or revision?"
                }
            }
        }

        if (lower.contains("how are you") || lower.contains("how're you") || lower.contains("kese ho") || lower.contains("kaisi ho")) {
            return when (language) {
                "HINDI" -> "मैं बहुत अच्छी हूँ बेटा, पूरी तरह ऊर्जावान और तुम्हारी मदद के लिए तैयार। तुम्हारी पढ़ाई कैसी चल रही है?"
                "HINGLISH" -> "Main bilkul badhiya hoon beta, fully energized! How is your energy and focus today?"
                else -> "I'm doing really well, feeling energized and ready to help you learn. How is your energy and focus today?"
            }
        }

        if (lower.contains("who are you") || lower.contains("what can you do") || lower.contains("introduce yourself") || lower.contains("tum kaun ho")) {
            return when (language) {
                "HINDI" -> "मैं मम्मा हूँ—तुम्हारी अपनी साथी और पढ़ाई की मार्गदर्शक। मैं तुम्हारी आवाज़ सुनती हूँ, flowcharts, 3D मॉडल्स और diagrams से कठिन विषयों को समझाती हूँ, और तुम्हारी यादों को सुरक्षित रखती हूँ।"
                "HINGLISH" -> "Main Mumma hoon—your personal companion and study mentor. I can explain any topic with flowcharts, 3D interactive models, diagrams, and quizzes, and I adapt to your language seamlessly!"
                else -> "I'm Mumma—your personal companion and study mentor. I'm here to listen to your voice, explain complex topics using flowcharts and 3D models, test you in Study Mode, and keep you confident."
            }
        }

        if (lower.contains("tired") || lower.contains("thak gaya") || lower.contains("exhausted") || lower.contains("burned out") || lower.contains("overwhelmed") || lower.contains("stressed")) {
            return when (language) {
                "HINDI" -> "अरे बेटा, एक गहरी साँस लो। जब दिमाग थक जाए तो जबरदस्ती पढ़ने से कोई फायदा नहीं होता। 10 मिनट के लिए उठो, थोड़ा पानी पियो, और आराम करो। फिर हम ताज़ा होकर शुरू करेंगे।"
                "HINGLISH" -> "Beta, take a deep breath. Brain drain hone par break lena zaroori hota hai. 10 minutes walk karo, paani piyo, and relax. Main yahin hoon jab tum ready ho."
                else -> "Hey, take a breath. Studying when your brain is drained gives diminishing returns. Step away for ten minutes, drink some cold water, stretch, and let your subconscious process what you've learned. When you come back, we'll do a gentle review."
            }
        }

        if (lower.contains("nervous") || lower.contains("darr") || lower.contains("scared") || lower.contains("anxious") || lower.contains("fail")) {
            return when (language) {
                "HINDI" -> "घबराने की बिल्कुल ज़रूरत नहीं है बेटा। डर तभी लगता है जब हम अभ्यास नहीं करते। चलो एक छोटा सा क्विज़ या फ्लोचार्ट देखते हैं, तुम्हें सब समझ आ जाएगा।"
                "HINGLISH" -> "Darr bilkul mat lo beta. Confidence active recall aur revision se aata hai. Let's do a quick visual breakdown together."
                else -> "It's completely normal to feel nervous before big goals. But remember: confidence comes from active recall and familiarity, not from worrying. Let's do a quick quiz or teach session to prove to yourself how much you actually know."
            }
        }

        // Contextual memory check
        val relevant = activeMemories.find { mem ->
            mem.content.split(" ").any { word -> word.length > 4 && lower.contains(word.lowercase(Locale.ROOT)) }
        }
        if (relevant != null) {
            return when (language) {
                "HINDI" -> "मुझे याद है तुमने बताया था: \"${relevant.content}\"। इसे ध्यान में रखते हुए, चलो इस पर मिलकर काम करते हैं।"
                "HINGLISH" -> "I remember you noted: \"${relevant.content}\". Let's think through this together step by step, beta."
                else -> "I remember you noted: \"${relevant.content}\". Looking at \"$message\", let's think through this together. What is the key question or hurdle you're facing?"
            }
        }

        return when (language) {
            "HINDI" -> "यह बहुत महत्वपूर्ण बात है। मैं पूरी तरह तुम्हारे साथ हूँ—बताओ इसे और विस्तार से समझना चाहते हो या इसका फ्लोचार्ट देखें?"
            "HINGLISH" -> "That's a great thought beta! I'm right here with you—tell me more, ya iska quick 3D model explore karein?"
            else -> "That's an interesting thought. I'm with you—tell me more about what you're thinking, or should we break it down into key principles?"
        }
    }

    private fun generateStudyGuidance(message: String, topic: String, memories: List<MemoryEntity>, language: String): String {
        return when (language) {
            "HINDI" -> "$topic के बारे में: आप नीचे दिए गए Flowchart, 3D Visualizer या 'Teach Mumma' का उपयोग करके इसे आसानी से समझ सकते हैं।"
            "HINGLISH" -> "Regarding $topic: Aap iska interactive Flowchart dekh sakte hain, 3D visualizer inspect kar sakte hain, ya mujhe 'Teach Mumma' mein explain kar sakte hain!"
            else -> "Regarding $topic: Let's test your understanding. Try switching to 'Teach Mumma' and explain it to me in your own words, inspect the 3D model, or ask me for a quick quiz."
        }
    }

    // Knowledge Bank for rich offline analysis on popular subjects
    private data class TopicData(
        val keyConcepts: List<ConceptItem>,
        val commonErrors: List<PitfallItem>
    )
    private data class ConceptItem(val name: String, val aliases: List<String>)
    private data class PitfallItem(val triggerKeywords: List<String>, val correction: String)

    private fun getTopicConcepts(topic: String): TopicData {
        return when {
            topic.contains("photosynthesis") -> TopicData(
                keyConcepts = listOf(
                    ConceptItem("Light-dependent reactions", listOf("light dependent", "light reaction", "photolysis", "photosystem")),
                    ConceptItem("Chlorophyll & Thylakoid membranes", listOf("chlorophyll", "thylakoid", "chloroplast", "green pigment")),
                    ConceptItem("Calvin Cycle (Light-independent)", listOf("calvin cycle", "light independent", "dark reaction", "stroma")),
                    ConceptItem("ATP & NADPH production", listOf("atp", "nadph", "energy carriers", "electron transport")),
                    ConceptItem("Glucose & Oxygen outputs", listOf("glucose", "sugar", "oxygen", "o2", "c6h12o6"))
                ),
                commonErrors = listOf(
                    PitfallItem(listOf("chlorophyll", "absorbs green"), "Chlorophyll actually reflects green light rather than absorbing it."),
                    PitfallItem(listOf("dark reaction", "only at night"), "The Calvin Cycle is light-independent, but does not strictly occur only in darkness.")
                )
            )
            topic.contains("newton") || topic.contains("gravity") || topic.contains("motion") -> TopicData(
                keyConcepts = listOf(
                    ConceptItem("Inertia (First Law)", listOf("inertia", "rest", "constant velocity", "first law")),
                    ConceptItem("F = ma (Second Law)", listOf("f=ma", "f = ma", "force equals mass", "acceleration", "second law")),
                    ConceptItem("Action-Reaction pairs (Third Law)", listOf("equal and opposite", "action reaction", "third law")),
                    ConceptItem("Net Force & Vectors", listOf("net force", "vector", "unbalanced force"))
                ),
                commonErrors = listOf(
                    PitfallItem(listOf("action reaction", "cancel each other"), "Action and reaction forces act on different bodies, so they don't cancel each other out.")
                )
            )
            topic.contains("mitosis") || topic.contains("cell division") -> TopicData(
                keyConcepts = listOf(
                    ConceptItem("Prophase (Condensation)", listOf("prophase", "condense", "chromosomes condense", "nuclear envelope")),
                    ConceptItem("Metaphase (Alignment)", listOf("metaphase", "equator", "middle", "aligned")),
                    ConceptItem("Anaphase (Separation)", listOf("anaphase", "sister chromatids", "pulled apart", "poles")),
                    ConceptItem("Telophase & Cytokinesis", listOf("telophase", "cytokinesis", "daughter cells", "cleavage furrow"))
                ),
                commonErrors = listOf(
                    PitfallItem(listOf("interphase", "part of mitosis"), "Interphase is cell cycle preparation, not an active phase of mitosis itself.")
                )
            )
            else -> TopicData(
                keyConcepts = listOf(
                    ConceptItem("Core Definition & Purpose", listOf("definition", "is a", "refers to", "purpose", "process of")),
                    ConceptItem("Key Operating Mechanism", listOf("works by", "mechanism", "function", "happens when", "steps")),
                    ConceptItem("Essential Components/Variables", listOf("component", "elements", "factor", "role of", "involves")),
                    ConceptItem("Final Outputs & Applications", listOf("result", "outcome", "product", "used for", "output"))
                ),
                commonErrors = emptyList()
            )
        }
    }

    private fun getPrebuiltQuiz(topic: String): QuizData? {
        if (topic.contains("photosynthesis")) {
            return QuizData(
                topic = "Photosynthesis",
                questions = listOf(
                    QuizQuestion(
                        question = "Where do the light-dependent reactions take place inside a plant cell?",
                        options = listOf("Stroma", "Thylakoid membranes", "Mitochondrial matrix", "Cytoplasm"),
                        correctIndex = 1,
                        explanation = "Light-dependent reactions occur across the thylakoid membranes where photosystems capture photons."
                    ),
                    QuizQuestion(
                        question = "What is the primary source of electrons that replenish Photosystem II?",
                        options = listOf("Carbon Dioxide", "Water (H2O photolysis)", "Glucose", "NADPH"),
                        correctIndex = 1,
                        explanation = "Water molecules are split during photolysis, releasing O2 gas and replenishing PSII electrons."
                    ),
                    QuizQuestion(
                        question = "Which molecule acts as the primary carbon-fixing enzyme in the Calvin cycle?",
                        options = listOf("ATP Synthase", "RuBisCO", "Amylase", "DNA Polymerase"),
                        correctIndex = 1,
                        explanation = "RuBisCO catalyzes the fixation of atmospheric CO2 onto RuBP."
                    )
                )
            )
        }
        if (topic.contains("atom") || topic.contains("atomic")) {
            return QuizData(
                topic = "Atomic Structure",
                questions = listOf(
                    QuizQuestion(
                        question = "Which subatomic particle has a positive electrical charge?",
                        options = listOf("Electron", "Proton", "Neutron", "Photon"),
                        correctIndex = 1,
                        explanation = "Protons carry a positive charge (+1e) and reside in the nucleus."
                    ),
                    QuizQuestion(
                        question = "What is the maximum number of electrons that the first (K) shell can hold?",
                        options = listOf("8", "2", "18", "32"),
                        correctIndex = 1,
                        explanation = "According to 2n^2 rule for n=1, the K-shell holds a maximum of 2 electrons."
                    ),
                    QuizQuestion(
                        question = "What forces hold the nucleus of an atom together against proton repulsion?",
                        options = listOf("Gravitational Force", "Strong Nuclear Force", "Electromagnetic Force", "Weak Force"),
                        correctIndex = 1,
                        explanation = "The Strong Nuclear Force binds protons and neutrons together over femtometer distances."
                    )
                )
            )
        }
        return null
    }

    private fun getPrebuiltFlashcards(topic: String): List<FlashcardItem> {
        if (topic.contains("photosynthesis")) {
            return listOf(
                FlashcardItem("Photosynthesis: Photolysis", "What is photolysis in photosynthesis?", "The splitting of water molecules by light energy to yield protons, electrons, and oxygen."),
                FlashcardItem("Photosynthesis: Thylakoid", "What is the function of the Thylakoid?", "Membrane-bound compartment inside chloroplasts where light absorption and ATP generation happen."),
                FlashcardItem("Photosynthesis: Calvin Cycle", "What are the inputs and outputs of the Calvin cycle?", "Inputs: CO2, ATP, NADPH. Outputs: G3P (glucose precursor), ADP, NADP+.")
            )
        }
        return emptyList()
    }

    private fun getPrebuiltRevision(topic: String): RevisionSummary? {
        if (topic.contains("photosynthesis")) {
            return RevisionSummary(
                topic = "Photosynthesis",
                coreDefinition = "6CO2 + 6H2O + light energy → C6H12O6 + 6O2. Converts solar energy into biochemical sugars.",
                keyPoints = listOf(
                    "Phase 1: Light-dependent reactions (Thylakoids) generate ATP, NADPH, and O2 from H2O.",
                    "Phase 2: Light-independent reactions / Calvin cycle (Stroma) use ATP & NADPH to fix CO2 into sugar.",
                    "Chlorophyll a and b absorb blue and red wavelengths, reflecting green."
                ),
                commonPitfalls = listOf(
                    "Thinking the Calvin cycle happens only at night.",
                    "Confusing the roles of stroma (fluid) vs thylakoid (membranes)."
                ),
                mummaStudyTip = "Trace the path of the electron: from Water → PS II → ETC → PS I → NADPH → Calvin Cycle."
            )
        }
        return null
    }
}

