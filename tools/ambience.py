"""
Prepares the recorded soundscapes: mono, 32 kHz (the app's mixing rate), loudness-matched to the
synthesised ones, Ogg Vorbis, into app/src/main/assets/ambience/<name>.ogg.

The recordings come from Blanket (github.com/rafaelmardojai/blanket, sounds from Freesound and
SoundBible; see the credits in README.md). Download them first, then:

    python tools/ambience.py <folder with blanket's .ogg files>
"""
import os
import subprocess
import sys

import imageio_ffmpeg

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'ambience')

# app name <- blanket file
SOUNDS = {
    'rain': 'rain.ogg',
    'ocean': 'waves.ogg',
    'stream': 'stream.ogg',
    'wind': 'wind.ogg',
    'forest': 'birds.ogg',
    'fire': 'fireplace.ogg',
    'night': 'summer-night.ogg',
    'storm': 'storm.ogg',
    'boat': 'boat.ogg',
    'city': 'city.ogg',
    'cafe': 'coffee-shop.ogg',
    'train': 'train.ogg',
    'white': 'white-noise.ogg',
    'pink': 'pink-noise.ogg',
}


def main():
    src = sys.argv[1]
    os.makedirs(OUT, exist_ok=True)
    ff = imageio_ffmpeg.get_ffmpeg_exe()
    for name, file in SOUNDS.items():
        out = os.path.join(OUT, name + '.ogg')
        subprocess.run([ff, '-y', '-loglevel', 'error', '-i', os.path.join(src, file),
                        '-ac', '1', '-af', 'highpass=f=40,loudnorm=I=-22:TP=-2:LRA=14', '-ar', '32000',
                        '-c:a', 'libvorbis', '-q:a', '3', out], check=True)
        print(name, os.path.getsize(out) // 1024, 'KB')


if __name__ == '__main__':
    main()
