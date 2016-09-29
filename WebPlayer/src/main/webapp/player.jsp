<%@ page import="org.bottiger.podcast.web.EpisodeModel" %>
<!doctype html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="x-ua-compatible" content="ie=edge">
    <meta name="description" content="The Podigee Podcast Player is a HTML5 audio player specially crafted for listening to podcasts.">
    <meta name="viewport" content="width=device-width, initial-scale=1">

    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.4.0/css/font-awesome.min.css">
    <link rel="stylesheet" href="example/normalize.css">
    <link rel="stylesheet" href="example/styles.css">

    <link href="https://fonts.googleapis.com/css?family=Roboto" rel="stylesheet">
        <style>
          body {
            font-family: 'Roboto', sans-serif;
          }
        </style>

    <script src="//code.jquery.com/jquery-1.11.3.min.js"></script>
    <!--
    <script>
    window.playerConfiguration = {
      "options": {
        "theme": "default",
        "startPanel": "Playlist"
      },
      "episode": {
        "media": {
          "mp3": "<%= EpisodeModel.getUrl(session) %>",
        },
        "coverUrl": "<%= EpisodeModel.getCover(session) %>",
        "title": "<%= EpisodeModel.getTitle(session) %>",
        "subtitle": "<%= EpisodeModel.getSubtitle(session) %>",
        "url": "<%= EpisodeModel.getWebsite(session) %>",
        "description": "<%= EpisodeModel.getDescription(session) %>",
      }
    }
    </script>
    -->
        <script>
        window.playerConfiguration = {
          "options": {
            "theme": "default",
            "startPanel": "Playlist"
          },
          "episode": {
            "media": {
              "mp3": "http://play.podtrac.com/npr-510289/npr.mc.tritondigital.com/NPR_510289/media/anon.npr-mp3/npr/pmoney/2016/08/20160819_pmoney_podcast081916.mp3?orgId=1&d=1480&p=510289&story=490403395&t=podcast&e=490403395&ft=pod&f=510289",
            },
            "coverUrl": "https://media.npr.org/assets/img/2016/08/19/planet-money-still-4_wide-e5ade61e4477c77209a6a3e49a285de118119a39.jpg?s=1400",
            "title": "Oil #4: How Oil Got Into Everything",
            "subtitle": "Oil #4: How Oil Got Into Everything",
            "url": "http://www.npr.org/rss/podcast.php?id=510289",
            "description": "Fourth of five episodes. Oil is in our sneakers, our clothes, and the computer or phone you're using right now. On today's show: The story of the man who made it happen.",
          }
        }
        </script>
  </head>
  <body>
      <p style="margin: 30px">
        <!--<script class="podigee-podcast-player" src="https://cdn.podigee.com/podcast-player/javascripts/podigee-podcast-player.js" data-configuration="playerConfiguration"></script>-->
        <script class="podigee-podcast-player" src="../player/javascripts/podigee-podcast-player.js" data-configuration="playerConfiguration"></script>
      </p>
      <hr style="height: 1px; border: none; color: #000;"/>
      <p id="player_state">
      Nothing
      </p>

      <script src="https://www.gstatic.com/firebasejs/3.3.0/firebase.js"></script>
      <script>
        // Initialize Firebase
        var config = {
          apiKey: "AIzaSyDZvX6mPV2Bpa4-W-AJILKi_bmPPmIwGEA",
          authDomain: "soundwaves-bottiger.firebaseapp.com",
          databaseURL: "https://soundwaves-bottiger.firebaseio.com",
          storageBucket: "soundwaves-bottiger.appspot.com",
        };
        firebase.initializeApp(config);
      </script>
  </body>
</html>