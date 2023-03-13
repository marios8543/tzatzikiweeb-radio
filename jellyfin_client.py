from requests import Session
from os import getenv

BASE_URL = getenv("JELLYFIN_BASE_URL")
TOKEN = getenv("JELLYFIN_TOKEN")
USER_ID = getenv("JELLYFIN_USER_ID")
MUSIC_LIBRARY_ID = getenv("JELLYFIN_MUSIC_LIBRARY_ID")

class HttpError(Exception):
    def __init__(self, text):
        Exception("Something went wrong: {}".format(text))

session = Session()
session.headers = {
    "x-emby-authorization":'MediaBrowser Client="Jellyfin CLI", Device="Jellyfin-CLI", DeviceId="None", Version="10.4.3", Token="{}"'.format(getenv("JELLYFIN_TOKEN"))
}

def ticks_to_seconds(ticks):
    return int(ticks*(1/10000000))

def get_music_library(offset=0, sort='DateCreated'):
    res = session.get(f"{BASE_URL}/Users/{USER_ID}/Items", params={
        'ParentId': MUSIC_LIBRARY_ID,
        'SortBy': sort,
        'IncludeItemTypes': 'Audio',
        'Fields': 'AudioInfo',
        'Limit': 100,
        'StartIndex': offset * 100,
    }).json()
    return [{
        "id": i["Id"],
        "album": i["Album"] if "Album" in i else "Unknown Album",
        "artist": i["AlbumArtist"] if "AlbumArtist" in i else "Unknown Artist",
        "title": i["Name"],
        "length": ticks_to_seconds(i["RunTimeTicks"])
    } for i in res["Items"]]

def search_music_library(query):
    res = session.get(f"{BASE_URL}/Users/{USER_ID}/Items", params={
        'ParentId': MUSIC_LIBRARY_ID,
        'SortBy': 'DateCreated,SortName',
        'IncludeItemTypes': 'Audio',
        'Fields': 'AudioInfo',
        'searchTerm': query
    }).json()
    return [{
        "id": i["Id"],
        "album": i["Album"] if "Album" in i else "Unknown Album",
        "artist": i["AlbumArtist"] if "AlbumArtist" in i else "Unknown Artist",
        "title": i["Name"],
        "length": ticks_to_seconds(i["RunTimeTicks"])
    } for i in res["Items"]]

def get_random_song():
    res = session.get(f"{BASE_URL}/Users/{USER_ID}/Items", params={
        'ParentId': MUSIC_LIBRARY_ID,
        'SortBy': 'Random',
        'IncludeItemTypes': 'Audio',
        'Fields': 'AudioInfo',
        'Limit': 1,
    }).json()
    return [{
        "id": i["Id"],
        "album": i["Album"] if "Album" in i else "Unknown Album",
        "artist": i["AlbumArtist"] if "AlbumArtist" in i else "Unknown Artist",
        "title": i["Name"],
        "length": ticks_to_seconds(i["RunTimeTicks"])
    } for i in res["Items"]][0]