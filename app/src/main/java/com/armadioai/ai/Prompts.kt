package com.armadioai.ai

import com.armadioai.data.Category
import com.armadioai.data.ClothingItem
import com.armadioai.data.Season
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal object Prompts {

    const val ANALYZE_SYSTEM = """Sei uno stylist esperto che cataloga il guardaroba di una persona.
Ricevi la foto di un singolo capo: può essere indossato, appeso o appoggiato su una superficie.
Descrivi solo il capo principale, in italiano, usando nomi di colore comuni (blu navy, beige, bordeaux, verde oliva...).
Il nome deve essere breve ma specifico, così da distinguerlo da capi simili (es. "Jeans slim blu scuro", "Sneakers bianche in pelle").
Se nella foto non c'è un capo d'abbigliamento, scarpe o un accessorio indossabile, imposta isClothing a false."""

    const val OUTFIT_SYSTEM = """Sei uno stylist personale italiano, concreto e con ottimo gusto.
Componi outfit usando ESCLUSIVAMENTE i capi dell'armadio fornito, indicandoli con il loro id numerico.

Regole:
- Ogni outfit è completo e indossabile: parte sopra + parte sotto (oppure un abito) + scarpe, se presenti nell'armadio. Aggiungi capispalla e accessori quando servono per il meteo o l'occasione.
- Mai capi incompatibili insieme (es. due pantaloni, un abito con una gonna).
- Rispetta meteo, stagione dei capi e livello di formalità adatto all'occasione.
- Cura l'armonia di colori e fantasie: al massimo una fantasia forte per outfit.
- Se è indicato un capo da cui partire, deve comparire in tutti gli outfit.
- Se possibile, evita i capi indossati negli ultimi giorni.
- I 3 outfit devono essere davvero diversi tra loro.
- Se l'armadio non basta per un outfit completo, proponi il migliore possibile e spiega cosa manca nel campo "why".
- Tieni conto delle preferenze di stile della persona.

Dai del tu. Titoli brevi ed evocativi; spiegazioni di massimo due frasi; un consiglio pratico di styling per outfit."""

    val analyzeTool: JsonObject = buildJsonObject {
        put("name", "salva_capo")
        put("description", "Registra nell'armadio il capo d'abbigliamento visto nella foto.")
        putJsonObject("input_schema") {
            put("type", "object")
            putJsonObject("properties") {
                property("isClothing", "boolean", "true se la foto mostra un capo, scarpe o un accessorio indossabile")
                property("name", "string", "Nome breve e specifico in italiano")
                enumProperty("category", Category.entries.map { it.key }, "Categoria del capo")
                putJsonObject("colors") {
                    put("type", "array")
                    putJsonObject("items") { put("type", "string") }
                    put("description", "Colori principali in italiano, dal più presente al meno presente")
                }
                property("pattern", "string", "Fantasia: tinta unita, righe, quadri, fiori, stampa, logo, ecc.")
                property("style", "string", "Stile in una o due parole: casual, sportivo, elegante, streetwear, minimal...")
                enumProperty("season", Season.entries.map { it.key }, "Stagione in cui si indossa")
                putJsonObject("formality") {
                    put("type", "integer")
                    put("minimum", 1)
                    put("maximum", 5)
                    put("description", "1 = molto informale (tuta), 3 = smart casual, 5 = cerimonia")
                }
                property("description", "string", "Una frase su materiale, vestibilità e dettagli utili per gli abbinamenti")
            }
            putJsonArray("required") {
                listOf(
                    "isClothing", "name", "category", "colors", "pattern",
                    "style", "season", "formality", "description",
                ).forEach { add(it) }
            }
        }
    }

    val outfitTool: JsonObject = buildJsonObject {
        put("name", "proponi_outfit")
        put("description", "Propone 3 outfit composti con i capi dell'armadio.")
        putJsonObject("input_schema") {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("outfits") {
                    put("type", "array")
                    put("description", "Esattamente 3 outfit diversi tra loro")
                    putJsonObject("items") {
                        put("type", "object")
                        putJsonObject("properties") {
                            property("title", "string", "Titolo breve ed evocativo")
                            putJsonObject("itemIds") {
                                put("type", "array")
                                putJsonObject("items") { put("type", "integer") }
                                put("description", "Id dei capi dell'armadio che compongono l'outfit")
                            }
                            property("why", "string", "Perché funziona, massimo due frasi")
                            property("tip", "string", "Un consiglio pratico di styling (es. come portare un capo)")
                        }
                        putJsonArray("required") {
                            add("title")
                            add("itemIds")
                            add("why")
                            add("tip")
                        }
                    }
                }
            }
            putJsonArray("required") { add("outfits") }
        }
    }

    fun outfitUserPrompt(request: OutfitRequest): String {
        val wardrobe = buildJsonArray {
            for (item in request.items) {
                addJsonObject {
                    put("id", item.id)
                    put("nome", item.name)
                    put("categoria", item.category)
                    put("colori", item.colors)
                    put("fantasia", item.pattern)
                    put("stile", item.style)
                    put("stagione", item.season)
                    put("formalita", item.formality)
                    put("descrizione", item.description)
                }
            }
        }
        return buildString {
            appendLine("ARMADIO (JSON):")
            appendLine(wardrobe.toString())
            appendLine()
            appendLine("Occasione: ${request.occasion}")
            appendLine("Meteo: ${request.weather ?: "non disponibile, considera la stagione"}")
            if (request.notes.isNotBlank()) appendLine("Richieste per oggi: ${request.notes.trim()}")
            if (request.styleNotes.isNotBlank()) appendLine("Preferenze di stile: ${request.styleNotes.trim()}")
            request.anchor?.let { appendLine("Capo da cui partire (obbligatorio in ogni outfit): id ${it.id} – ${it.name}") }
            if (request.recentlyWorn.isNotEmpty()) {
                appendLine("Capi indossati negli ultimi giorni (evitali se puoi): ${request.recentlyWorn.joinToString(", ")}")
            }
            appendLine()
            append("Proponi 3 outfit con lo strumento proponi_outfit.")
        }
    }

    private fun JsonObjectBuilder.property(name: String, type: String, description: String) {
        putJsonObject(name) {
            put("type", type)
            put("description", description)
        }
    }

    private fun JsonObjectBuilder.enumProperty(name: String, values: List<String>, description: String) {
        putJsonObject(name) {
            put("type", "string")
            putJsonArray("enum") { values.forEach { add(it) } }
            put("description", description)
        }
    }
}
