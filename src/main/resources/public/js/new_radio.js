const player = document.getElementById("mainAudio");
player.volume = 0.25;
let ws = null;
const app = new Vue({
    'el': '#mainWrap',
    data: {
        upcoming: [],
        nowPlaying: {},
        messages:[{author:"",content:"Welcome to tzatzikiweeb radio"}],
        nowListening:0,
        chatInput:"",
        searchInput:"",
        times: {
            currentTime:0,
            duration:0
        },
        muted:false,
        firstPlay:false,

        songList: [],
        offset_count: 0,
        current_offset: 0,
        sortBy: 1,

        skipVotes:"",
        showSkipVotes:false
    },
    methods: {
        timeFormat (time) {
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
        startPlayback (time = null) {
            this.showSkipVotes = false;
            this.skipVotes = "";

            player.src = "https://cdn.tzatzikiweeb.moe/file/tzatzikiweeb-public/music" + encodeURIComponent(this.nowPlaying.path);
            this.times.duration = this.nowPlaying.length;
            player.play();
            if (time != null) player.currentTime = time;
        },
        fetchSongs (firstTime = false, noPlayback = false) {
            let _this = this
            $.getJSON("api/radio", {}, function (data) {
                app.nowPlaying = data.song;
                app.upcoming = data.upcoming;
                if (noPlayback) return;
                if (firstTime) _this.startPlayback(data.time);
                else _this.startPlayback();
            });
        },
        sendMessage () {
            if (this.chatInput.length>0) {
                ws.send(this.chatInput);
                this.chatInput = "";
            }
        },
        mute () {
            if (player.muted) player.muted = false;
            else player.muted = true;
            this.muted = player.muted;
        },
        updateVolume (event) {
            player.volume = event.target.value/100;
        },
        loadRange (i = app.current_offset) {
            $.getJSON("api/player", { 's': i, 'sortBy': app.sortBy, 'q': app.searchInput }, function (res) {
                app.songList = res.songs;
                app.offset_count = res.offset_count;
                app.current_offset = i;
            });
        },
        selectSort (sort) {
            app.sortBy = sort;
            app.current_offset = 0;
            this.loadRange()
        },
        submitRequest (id) {
            let _this = this;
            $.getJSON("api/radio/request", {id:id}, function (res) {
                _this.fetchSongs(noPlayback = true);
            }).fail(function (xhr, status, error) {
            });
        },
        voteSkip () {
            let _this = this;
            $.getJSON("api/radio/voteSkip", function (res) {
                _this.showSkipVotes = true;
            }).fail(function (xhr, status, error) {
            });
        },
        pageInput () {
            let page = parseInt(prompt("Enter page"));
            if (page >= 0 && page <= app.offset_count) {
                this.loadRange(page);
            }
        },
        onImgError (e) {
            e.target.src = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAJwAAACcCAMAAAC9ZjJ/AAAAP1BMVEXv6++cmpzW09acnpze297n4+etqq3n5+fe396trq3W19alpqWloqW9ur3Oy861trXOz87Gx8a1srW9vr3Gw8Z92mNsAAAC0UlEQVR4nO3b65KiMBCGYSXkAAQ5yP1f64rZrfWQdtLthqS2vvf3UD4jGBDaU1Nxp+ZUbcBJA04acNKAkwacNOCkAScNOGnASQNOGnDSgJMGnDTgpAEn7SicdX2vuRsdgLOuWaZuUMr0zC3z4u4u49U51Fne5tlwu2v867rn2/K42/G1jN2z654qiyNdhXE312W8HfdxVzlcgqsILtV1PM4211TX4Tg9MWBH46482qE4PVSMa33NOOYRBxxwwAH33+LU0BlVIe7mGi+9s3ZRVeH+uMIfu6EW3LPrnjYV4CKuKnCUqzTuo6skzv/kKohb26QbIEVwqbc/iuCGxJtuRXAGOH7A8Vkh4PisEHB8Vgg4PisEHJ8VAo7PCgHHZ4WA47NCwPFZIeD4rBAPV+IWWCrO+Ypxy7lanL74KnH35+zhfnVdON3OT8/Z68HZ/mpensjWgrNNV3Qi5xNOj9G72cfhehqn1wjtUNxM4uwYtR2Is9OZws2lB630piic7uK2Q3B2X79+r6sxXEM9d8qNs67Z1oeBwhiOnNTJOnlo++11cC+Cs/GP6q3BZcPZeX3fXzEcdcidTbaBUhed+WLhRp4tHdfGXzKGmwibmjPhiDU/+oFYqL3KHbFOxW2MFyTGw9TCtKXiHDUrF8MRZ68112z6hbDFd1Ub+1cMc5FLxpHHODFuPr/vWPbUfDrOULg1irOvOjUy118Gjh7PvBJb9A8Lthqmhrn8snDkO0cuXbYZjfd+MOs2/zgB8BWOXPQ/nsq1a53TQlg67kRc2p6nL176n+GIa1uV9zcyiTjioBuzvnHJp69L7K3rJOtDBlzslJTbln7J9DbC76/scyW39ItNuzyuxH7q8x5ve5zvEO3+DUIpv498SddVVrxvX/uP8vr2m3WVFX4IKQ04acBJA04acNKAkwacNOCkAScNOGnASQNOGnDSgJMGnLTacfW2/QIOcSFXoUVQewAAAABJRU5ErkJggg=="
        }
    },
    mounted: function () {
        this.$nextTick(function () {
            $("#openPopup").click();
        });

        $.getJSON("api/player", { 's': 0, 'sortBy': 1 }, function (res) {
            app.songList = res.songs;
            app.offset_count = res.offset_count;
        });
    }
});

var toggle = false;
function connectWs() {
	ws = new WebSocket('wss://radio.tzatzikiweeb.moe/api/chat')
	ws.onmessage = function(event) {
    	    packet = JSON.parse(event.data);
    	    if(packet.author=="force_reload") {
                setTimeout(function() {
                    app.fetchSongs();
                },1500);
                return;
            }
            if(packet.author=="skip_votes") {
                app.skipVotes = packet.content;
                return;
            }
            if(toggle){
                toggle = false;
                return;
            }
            app.nowListening = packet.nowListening;
            delete packet.nowListening;
            app.messages.push(packet);
            if (app.messages.length > 50) app.messages.shift();
        }

        ws.onclose = function(event) {
            toggle = true;
            connectWs();
        }
}

connectWs();

player.onended = function () {
    setTimeout(function() {
        app.fetchSongs();
    },3000)
}

player.ontimeupdate = function () {
    app.times.currentTime = player.currentTime;
}
