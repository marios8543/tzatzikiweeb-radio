const player = document.getElementById("mainPlayer");
const app = new Vue({
    el: '#mainWrap',
    data: {
        songList: [],
        offset_count: 0,
        current_offset: 0,
        sortBy: 1,
        now_playing: {
            idx: 0,
            song: {
                title:"UwU",
                artist:"Welcome",
                album:"--",
                id:0
            }
        },
        currentTime: 0,
        duration: 0,
        pbar: 0,
        player: player,
        search_query:""
    },
    methods: {
        time_format: function (time) {
            var hrs = ~~(time / 3600);
            var mins = ~~((time % 3600) / 60);
            var secs = ~~time % 60;
            var ret = "";
            if (hrs > 0) {
                ret += "" + hrs + ":" + (mins < 10 ? "0" : "");
            }
            ret += "" + mins + ":" + (secs < 10 ? "0" : "");
            ret += "" + secs;
            return ret;
        },
        loadRange: function (i = app.current_offset) {
            $.getJSON("api/player", { 's': i, 'sortBy': app.sortBy }, function (res) {
                app.songList = res.songs;
                app.offset_count = res.offset_count;
                app.current_offset = i;
            });
        },
        play: function (index) {
            app.now_playing.song = app.songList[index];
            app.now_playing.idx = index;
            player.src = 'api/fetchSong?id=' + app.songList[index].id;
            player.play() !== null ? player.play().catch(function (err) {
                if (err.name == "NotSupportedError") {
                    player.src = 'api/transcode?id=' + app.songList[index].id;
                    player.play();
                }
            }) : false;
        },
        selectSort: function (sort) {
            app.sortBy = sort;
            app.current_offset = 0;
            this.loadRange()
        },
        playPause: function () {
            if (player.paused || player.ended) player.play()
            else player.pause()
        },
        next: function () {
            this.play(app.now_playing.idx + 1 < app.songList.length ? app.now_playing.idx + 1 : 0);
        },
        previous: function () {
            this.play(app.now_playing.idx - 1 >= 0 ? app.now_playing.idx - 1 : 0);
        },
        stop: function () {
            player.pause();
            player.currentTime = 0;
        },
        search: function () {
            $.getJSON("api/search", { 'q': app.search_query }, function (res) {
                app.songList = res.songs;
            });
        }
    },
    mounted: function () {
        $.getJSON("api/player", { 's': 0, 'sortBy': 1 }, function (res) {
            app.songList = res.songs;
            app.offset_count = res.offset_count;
        });
    }
});

player.ontimeupdate = function () {
    app.pbar = Math.floor((100 * player.currentTime) / player.duration);
    app.currentTime = player.currentTime;
    app.duration = player.duration;
}
