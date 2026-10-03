package com.example.data.model

enum class PresetCategory(val label: String) {
    ALL("Semua"),
    ARTISTIC("Gaya Seni"),
    BACKGROUND("Ganti Latar"),
    PORTRAIT("Potret & Efek"),
    ENHANCE("Perbaikan Foto")
}

data class EditPreset(
    val id: String,
    val title: String,
    val category: PresetCategory,
    val description: String,
    val prompt: String,
    val iconEmoji: String
)

object PresetRepository {
    val presets = listOf(
        EditPreset(
            id = "anime",
            title = "Anime Jepang",
            category = PresetCategory.ARTISTIC,
            description = "Gaya animasi anime dengan warna cerah dan garis halus",
            prompt = "Transform this photo into a vibrant modern Japanese anime illustration style. Keep the subject identifiable with expressive eyes, clean inked line art, and luminous celestial coloring.",
            iconEmoji = "✨"
        ),
        EditPreset(
            id = "oil_paint",
            title = "Lukisan Cat Minyak",
            category = PresetCategory.ARTISTIC,
            description = "Tekstur kanvas klasik dengan sapuan kuas impresionis",
            prompt = "Transform this photo into an impressionist classical oil painting on canvas. Emphasize visible rich textured brush strokes, warm classical palette, and museum masterpiece aesthetic.",
            iconEmoji = "🎨"
        ),
        EditPreset(
            id = "cyberpunk",
            title = "Cyberpunk Neon",
            category = PresetCategory.ARTISTIC,
            description = "Cahaya neon futuristik biru cyan dan ungu magenta",
            prompt = "Convert this photo into a cinematic cyberpunk futuristic aesthetic. Add vibrant glowing neon lights in cyan and magenta, subtle holographic reflections, and atmospheric high-tech noir vibe.",
            iconEmoji = "🏙️"
        ),
        EditPreset(
            id = "pencil_sketch",
            title = "Sketsa Pensil",
            category = PresetCategory.ARTISTIC,
            description = "Gambar pensil grafit arang hitam putih artistik",
            prompt = "Transform this photo into a masterfully shaded fine-art graphite pencil sketch drawing on textured ivory paper with crosshatching and expressive contours.",
            iconEmoji = "✏️"
        ),
        EditPreset(
            id = "watercolor",
            title = "Lukisan Cat Air",
            category = PresetCategory.ARTISTIC,
            description = "Gradasi cat air lembut dengan cipratan transparan",
            prompt = "Transform this photo into a delicate, luminous watercolor painting with soft translucent pastel washes, gentle pigment bleeding, and textured watercolor paper edges.",
            iconEmoji = "🌊"
        ),
        EditPreset(
            id = "pixar_3d",
            title = "Animasi 3D",
            category = PresetCategory.ARTISTIC,
            description = "Karakter animasi 3D kartun modern yang menggemaskan",
            prompt = "Turn the subject in this photo into a stylized 3D animated film character, similar to high-budget modern animation studios, with expressive features and warm studio lighting.",
            iconEmoji = "🧸"
        ),
        EditPreset(
            id = "bg_sunset_beach",
            title = "Pantai Tropis Sunset",
            category = PresetCategory.BACKGROUND,
            description = "Ganti latar belakang dengan pantai emas tropis",
            prompt = "Seamlessly keep the main foreground subject exactly intact, but replace the entire background with an exotic tropical white-sand beach at breathtaking golden sunset with coconut palms and gentle ocean waves.",
            iconEmoji = "🏖️"
        ),
        EditPreset(
            id = "bg_future_city",
            title = "Kota Masa Depan",
            category = PresetCategory.BACKGROUND,
            description = "Latar belakang kota sci-fi metropolitan megah",
            prompt = "Keep the main subject perfectly preserved, and change the background to a breathtaking futuristic sci-fi city with flying sky-cars, sleek glass spires, and ambient neon skylines.",
            iconEmoji = "🚀"
        ),
        EditPreset(
            id = "bg_studio",
            title = "Studio Minimalis",
            category = PresetCategory.BACKGROUND,
            description = "Latar belakang studio foto profesional yang bersih",
            prompt = "Keep the main subject sharp and clear, replace the background with an ultra-clean minimalist luxury photo studio with smooth concrete backdrop and soft diffuse studio softbox lighting.",
            iconEmoji = "📸"
        ),
        EditPreset(
            id = "bg_snow_mountain",
            title = "Gunung Salju Alpin",
            category = PresetCategory.BACKGROUND,
            description = "Pemandangan puncak salju di bawah langit biru",
            prompt = "Carefully keep the main subject, replace the background with majestic snow-covered alpine mountain peaks and pristine glaciers under an azure sunny sky.",
            iconEmoji = "🏔️"
        ),
        EditPreset(
            id = "bg_sakura_garden",
            title = "Taman Sakura",
            category = PresetCategory.BACKGROUND,
            description = "Pemandangan musim semi dengan kelopak bunga sakura",
            prompt = "Keep the subject intact, replace the background with a scenic Japanese spring garden filled with blooming pink sakura cherry blossom trees and falling petals.",
            iconEmoji = "🌸"
        ),
        EditPreset(
            id = "golden_hour",
            title = "Golden Hour Glow",
            category = PresetCategory.PORTRAIT,
            description = "Pencahayaan matahari sore hangat dan dramatis",
            prompt = "Enhance the lighting in this photo to a magical golden hour sunset light, adding warm golden rim light on the subject, soft natural lens flares, and dreamy radiant warm tones.",
            iconEmoji = "🌅"
        ),
        EditPreset(
            id = "sunglasses_jacket",
            title = "Kacamata & Jaket Keren",
            category = PresetCategory.PORTRAIT,
            description = "Tambahkan kacamata hitam trendi dan aksesoris",
            prompt = "Naturally add stylish sleek black designer sunglasses to the subject's face and dress them in a fashionable modern streetwear leather jacket, seamlessly integrated.",
            iconEmoji = "🕶️"
        ),
        EditPreset(
            id = "vintage_film",
            title = "Vintage Film 1980",
            category = PresetCategory.PORTRAIT,
            description = "Sentuhan film analog 35mm klasik dengan grain hangat",
            prompt = "Give this photo the authentic aesthetic of 1980s 35mm film photography, with rich warm organic grain, soft nostalgic highlight glow, subtle chromatic aberration, and retro charm.",
            iconEmoji = "🎞️"
        ),
        EditPreset(
            id = "cinematic_noir",
            title = "Sinematik Noir",
            category = PresetCategory.PORTRAIT,
            description = "Pencahayaan dramatis misterius ala film klasik",
            prompt = "Transform this photo into dramatic cinematic noir lighting, with high contrast shadows, razor-sharp light beams cutting through darkness, and a mysterious movie poster atmosphere.",
            iconEmoji = "🎬"
        ),
        EditPreset(
            id = "enhance_clarity",
            title = "Restorasi HD AI",
            category = PresetCategory.ENHANCE,
            description = "Tingkatkan ketajaman, detail, dan kejernihan warna",
            prompt = "Professionally restore and remaster this image. Enhance micro-contrast, sharpen intricate textures, correct lighting exposure, and make colors pop with ultra-high clarity.",
            iconEmoji = "🔮"
        )
    )
}
