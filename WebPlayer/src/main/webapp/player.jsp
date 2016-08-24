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

    <script src="//code.jquery.com/jquery-1.11.3.min.js"></script>
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
  </head>
  <body>
      <p style="margin: 30px">
        <script class="podigee-podcast-player" src="https://cdn.podigee.com/podcast-player/javascripts/podigee-podcast-player.js" data-configuration="playerConfiguration"></script>
      </p>
      <hr />
  </body>
</html>