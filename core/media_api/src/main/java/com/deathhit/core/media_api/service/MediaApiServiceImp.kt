package com.deathhit.core.media_api.service

import com.deathhit.core.media_api.model.Media
import kotlinx.coroutines.delay
import kotlin.random.Random

internal class MediaApiServiceImp : MediaApiService {
    private val mediaList = listOf(
        Media(
            "Big Buck Bunny tells the story of a giant rabbit with a heart bigger than himself. When one sunny day three rodents rudely harass him, something snaps... and the rabbit ain't no bunny anymore! In the typical cartoon tradition he prepares the nasty rodents a comical revenge.\n\nLicensed under the Creative Commons Attribution license\nhttp://www.bigbuckbunny.org",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            "By Blender Foundation",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/BigBuckBunny.jpg",
            "Big Buck Bunny"
        ),
        Media(
            "The first Blender Open Movie from 2006",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
            "By Blender Foundation",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ElephantsDream.jpg",
            "Elephant Dream"
        ),
        Media(
            "HBO GO now works with Chromecast -- the easiest way to enjoy online video on your TV. For when you want to settle into your Iron Throne to watch the latest episodes. For $35.\nLearn how to use Chromecast with HBO GO and more at google.com/chromecast.",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
            "By Google",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg",
            "For Bigger Blazes"
        ),
        Media(
            "Introducing Chromecast. The easiest way to enjoy online video and music on your TV—for when Batman's escapes aren't quite big enough. For $35. Learn how to use Chromecast with Google Play Movies and more at google.com/chromecast.",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
            "By Google",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerEscapes.jpg",
            "For Bigger Escape"
        ),
        Media(
            "Introducing Chromecast. The easiest way to enjoy online video and music on your TV. For $35.  Find out more at google.com/chromecast.",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
            "By Google",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerFun.jpg",
            "For Bigger Fun"
        ),
        Media(
            "Introducing Chromecast. The easiest way to enjoy online video and music on your TV—for the times that call for bigger joyrides. For $35. Learn how to use Chromecast with YouTube and more at google.com/chromecast.",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",
            "By Google",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerJoyrides.jpg",
            "For Bigger Joyrides"
        ),
        Media(
            "Introducing Chromecast. The easiest way to enjoy online video and music on your TV—for when you want to make Buster's big meltdowns even bigger. For $35. Learn how to use Chromecast with Netflix and more at google.com/chromecast.",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
            "By Google",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerMeltdowns.jpg",
            "For Bigger Meltdowns"
        ),
        Media(
            "Sintel is an independently produced short film, initiated by the Blender Foundation as a means to further improve and validate the free/open source 3D creation suite Blender. With initial funding provided by 1000s of donations via the internet community, it has again proven to be a viable development model for both open 3D technology as for independent animation film.\nThis 15 minute film has been realized in the studio of the Amsterdam Blender Institute, by an international team of artists and developers. In addition to that, several crucial technical and creative targets have been realized online, by developers and artists and teams all over the world.\nwww.sintel.org",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
            "By Blender Foundation",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/Sintel.jpg",
            "Sintel"
        ),
        Media(
            "Smoking Tire takes the all-new Subaru Outback to the highest point we can find in hopes our customer-appreciation Balloon Launch will get some free T-shirts into the hands of our viewers.",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4",
            "By Garage419",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/SubaruOutbackOnStreetAndDirt.jpg",
            "Subaru Outback On Street And Dirt"
        ),
        Media(
            "Tears of Steel was realized with crowd-funding by users of the open source 3D creation tool Blender. Target was to improve and test a complete open and free pipeline for visual effects in film - and to make a compelling sci-fi film in Amsterdam, the Netherlands.  The film itself, and all raw material used for making it, have been released under the Creatieve Commons 3.0 Attribution license. Visit the tearsofsteel.org website to find out more about this, or to purchase the 4-DVD box with a lot of extras.  (CC) Blender Foundation - http://www.tearsofsteel.org",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
            "By Blender Foundation",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/TearsOfSteel.jpg",
            "Tears of Steel"
        ),
        Media(
            "The Smoking Tire heads out to Adams Motorsports Park in Riverside, CA to test the most requested car of 2010, the Volkswagen GTI. Will it beat the Mazdaspeed3's standard-setting lap time? Watch and see...",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4",
            "By Garage419",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/VolkswagenGTIReview.jpg",
            "Volkswagen GTI Review"
        ),
        Media(
            "The Smoking Tire is going on the 2010 Bullrun Live Rally in a 2011 Shelby GT500, and posting a video from the road every single day! The only place to watch them is by subscribing to The Smoking Tire or watching at BlackMagicShine.com",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
            "By Garage419",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/WeAreGoingOnBullrun.jpg",
            "We Are Going On Bullrun"
        ),
        Media(
            "The Smoking Tire meets up with Chris and Jorge from CarsForAGrand.com to see just how far $1,000 can go when looking for a car.The Smoking Tire meets up with Chris and Jorge from CarsForAGrand.com to see just how far $1,000 can go when looking for a car.",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/WhatCarCanYouGetForAGrand.mp4",
            "By Garage419",
            "https://storage.googleapis.com/gtv-videos-bucket/sample/images/WhatCarCanYouGetForAGrand.jpg",
            "What care can you get for a grand?"
        )
    )

    override suspend fun getMediaList(page: Int?, pageSize: Int): List<Media> {
        delay(Random.nextLong(500))

        val offset = (page ?: MediaApiService.DEFAULT_PAGE) * pageSize
        val limit = offset + pageSize

        return mediaList.subList(
            if (offset > mediaList.lastIndex) mediaList.lastIndex else offset,
            if (limit > mediaList.size) mediaList.size else limit
        )
    }
}