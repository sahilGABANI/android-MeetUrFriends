package com.meetfriend.app.utils

import com.meetfriend.app.R
import com.meetfriend.app.api.effect.model.EffectResponse

class DeeparDetails {
    companion object {

        const val NONE: String = "None"
        fun getMasks(): ArrayList<EffectResponse> {
            val listofItems: ArrayList<EffectResponse> = arrayListOf()
            listofItems.add(EffectResponse(1, NONE, "", R.drawable.none, "mask"))
            listofItems.add(EffectResponse(2, "MakeupLook", "MakeupLook.deepar", R.drawable.ic_face_beauty, "mask"))
            listofItems.add(EffectResponse(3, "Split View Look", "Split_View_Look.deepar", R.drawable.ic_half_face_beauty, "mask"))
            listofItems.add(EffectResponse(4, "Shiny Glitter Face", "shiny-glitter-face.deepar", R.drawable.shiny_glitter_face, "mask"))
            listofItems.add(EffectResponse(5, "Bright glasses", "bright-glasses.deepar", R.drawable.bright_glasses, "mask"))
            listofItems.add(EffectResponse(6, "aviators", "aviators", R.drawable.aviators, "mask"))
            listofItems.add(EffectResponse(7, "flower_face", "flower_face.deepar", R.drawable.ic_face_flower, "mask"))
            listofItems.add(EffectResponse(8, "small-flowers", "small-flowers.deepar", R.drawable.small_flowers, "mask"))
            listofItems.add(EffectResponse(9, "flowers", "flowers", R.drawable.flowers, "mask"))
            listofItems.add(EffectResponse(10, "Butterfly_Headband", "butterfly-headband.deepar", R.drawable.butterfly_headband, "mask"))
            listofItems.add(EffectResponse(11, "Neon_Devil_Horns", "Neon_Devil_Horns.deepar", R.drawable.ic_horns, "mask"))
            listofItems.add(EffectResponse(12, "sequin-butterfly", "sequin-butterfly.deepar", R.drawable.sequin_butterfly, "mask"))
            listofItems.add(EffectResponse(13, "spring-fairy", "spring-fairy.deepar", R.drawable.spring_fairy, "mask"))
            listofItems.add(EffectResponse(14, "spring-deer", "spring-deer.deepar", R.drawable.sprin_deer,"mask"))
            listofItems.add(EffectResponse(15, "spring-tree", "spring-tree.deepar", R.drawable.spring_tree, "mask"))
            listofItems.add(EffectResponse(16, "troll-funk", "troll-funk.deepar", R.drawable.troll_funk, "mask"))
            listofItems.add(EffectResponse(17, "troll-reggaeton", "troll-reggaeton.deepar", R.drawable.troll_reggaeton, "mask"))
            listofItems.add(EffectResponse(18, "amanita-look.deepar", "amanita-look.deepar", R.drawable.amanita_look, "mask"))
            listofItems.add(EffectResponse(19, "bunny-ears.deepar", "bunny-ears.deepar", R.drawable.bunny_ears, "mask"))
            listofItems.add(EffectResponse(20, "dalmatian", "dalmatian", R.drawable.dalmatian, "mask"))
            listofItems.add(EffectResponse(21, "Humanoid.deepar", "Humanoid.deepar", R.drawable.ic_robotic, "mask"))
            listofItems.add(EffectResponse(22, "cracked-porcelain-face.deepar", "cracked-porcelain-face.deepar", R.drawable.cracked_porcelain_face, "mask"))
            listofItems.add(EffectResponse(23, "twisted-tongue.deepar", "twisted-tongue.deepar", R.drawable.twisted_tongue, "mask"))
            listofItems.add(EffectResponse(24, "twistedFace", "twistedFace", R.drawable.twisted_face, "mask"))
            listofItems.add(EffectResponse(25, "bigmouth", "bigmouth", R.drawable.bigmouth, "mask"))
            listofItems.add(EffectResponse(26, "fatify", "fatify", R.drawable.fatify, "mask"))
            listofItems.add(EffectResponse(27, "smallface", "smallface", R.drawable.smallface, "mask"))
            listofItems.add(EffectResponse(28, "tripleface", "tripleface", R.drawable.tripleface, "mask"))
            listofItems.add(EffectResponse(29, "derp.deepar", "derp.deepar", R.drawable.derp, "mask"))
            listofItems.add(EffectResponse(30, "extreme-makeover.deepar", "extreme-makeover.deepar", R.drawable.extreme_makeover, "mask"))
            listofItems.add(EffectResponse(31, "Stallone.deepar", "Stallone.deepar", R.drawable.ic_face_make, "mask"))
            listofItems.add(EffectResponse(32, "grumpycat", "grumpycat", R.drawable.grumpycat, "mask"))
            listofItems.add(EffectResponse(33, "kanye.deepar", "kanye", R.drawable.kanye, "mask"))
            listofItems.add(EffectResponse(34, "mudMask", "mudMask", R.drawable.mud_mask, "mask"))
            listofItems.add(EffectResponse(35, "obama", "obama", R.drawable.obama, "mask"))
            listofItems.add(EffectResponse(36, "slash", "slash", R.drawable.slash, "mask"))
            listofItems.add(EffectResponse(37, "sleepingmask", "sleepingmask", R.drawable.sleepingmask, "mask"))
            listofItems.add(EffectResponse(38, "nfl-helmet-red.deepar", "nfl-helmet-red.deepar", R.drawable.nfl_helmet_red, "mask"))
            listofItems.add(EffectResponse(39, "viking_helmet.deepar", "viking_helmet.deepar", R.drawable.ic_helmet, "mask"))
            listofItems.add(EffectResponse(40, "lightbulb.deepar", "lightbulb.deepar", R.drawable.lightbulb, "mask"))
            listofItems.add(EffectResponse(41, "harry-potter.deepar", "harry-potter.deepar", R.drawable.harry_potter, "mask"))
            listofItems.add(EffectResponse(42, "santa.deepar", "santa.deepar", R.drawable.santa, "mask"))
            listofItems.add(EffectResponse(43, "teddycigar", "teddycigar", R.drawable.teddycigar, "mask"))
            listofItems.add(EffectResponse(44, "Emotions_Exaggerator.deepar", "Emotions_Exaggerator.deepar", R.drawable.ic_emotional, "mask"))
            listofItems.add(EffectResponse(45, "Emotion_Meter.deepar", "Emotion_Meter.deepar", R.drawable.ic_rainbow, "mask"))
            listofItems.add(EffectResponse(46, "Ping_Pong.deepar", "Ping_Pong.deepar", R.drawable.ic_table_tennis, "mask"))
            listofItems.add(EffectResponse(47, "Hope.deepar", "Hope.deepar", R.drawable.ic_reverse_effect, "mask"))
            listofItems.add(EffectResponse(48, "Vendetta_Mask", "Vendetta_Mask.deepar", R.drawable.ic_face_mask, "mask"))
            listofItems.add(EffectResponse(49, "burning_effect", "burning_effect.deepar", R.drawable.ic_background_fire, "mask"))
            listofItems.add(EffectResponse(50, "Face_Swap", "Face_Swap", R.drawable.face_swap, "mask"))
            listofItems.add(EffectResponse(51, "Snail", "Snail.deepar", R.drawable.ic_squirrel, "mask"))
            listofItems.add(EffectResponse(52, "koala", "koala", R.drawable.koala, "mask"))
            listofItems.add(EffectResponse(53, "lion", "lion", R.drawable.lion, "mask"))
            listofItems.add(EffectResponse(54, "pug", "pug", R.drawable.pug, "mask"))
            listofItems.add(EffectResponse(55, "Elephant_Trunk", "Elephant_Trunk.deepar", R.drawable.ic_elephant, "mask"))
            listofItems.add(EffectResponse(56, "cute-virtual-pet.deepar", "cute-virtual-pet.deepar", R.drawable.cute_virtual_pet, "mask"))

            return listofItems
        }

        fun getEffects(): ArrayList<EffectResponse> {
            val listofItems: ArrayList<EffectResponse> = arrayListOf()
            listofItems.add(EffectResponse(1, NONE, "", R.drawable.none, "effects"))
            listofItems.add(EffectResponse(2, "tv80", "tv80", R.drawable.tv80, "effects"))
            listofItems.add(EffectResponse(3, "drawingmanga", "drawingmanga", R.drawable.drawingmanga, "effects"))
            listofItems.add(EffectResponse(4, "sepia", "sepia", R.drawable.sepia, "effects"))
            listofItems.add(EffectResponse(5, "bleachbypass", "bleachbypass", R.drawable.bleachbypass, "effects"))
            listofItems.add(EffectResponse(6, "realvhs", "realvhs", R.drawable.realvhs, "effects"))
            listofItems.add(EffectResponse(7, "filmcolorperfection", "filmcolorperfection", R.drawable.filmcolorperfection, "effects"))

            return listofItems
        }

        fun getFilters(): ArrayList<EffectResponse> {
            val listofItems: ArrayList<EffectResponse> = arrayListOf()
            listofItems.add(EffectResponse(1, NONE, "", R.drawable.none, "filters"))
            listofItems.add(EffectResponse(2, "Fire_Effect", "Fire_Effect.deepar", R.drawable.ic_background_fire, "filters"))
            listofItems.add(EffectResponse(3, "Pixel Hearts", "Pixel_Hearts.deepar", R.drawable.ic_love, "filters"))
            listofItems.add(EffectResponse(4, "Blizzard", "blizzard", R.drawable.blizzard, "filters"))
            listofItems.add(EffectResponse(5, "Rain", "rain", R.drawable.rain, "filters"))

            return listofItems
        }

        fun getBackground(): ArrayList<EffectResponse> {
            val listofItems: ArrayList<EffectResponse> = arrayListOf()
            listofItems.add(EffectResponse(1, NONE, "", R.drawable.none, "mask"))
            listofItems.add(EffectResponse(2, "galaxy_background", "galaxy_background.deepar", R.drawable.ic_galaxy, "mask"))

            return listofItems
        }
    }
}