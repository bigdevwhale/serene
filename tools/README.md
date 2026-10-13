# Narration

`narrate.py` records the studio voice for every stock line (guided steps, vision-board prompts,
affirmations) into `app/src/main/assets/voice/<lang>/<hash>.webm`. The app finds a recording by the
SHA-1 of the exact text (`Narrator.key`), so a line that was edited simply falls back to the phone's
voice until it is recorded again.

```bash
python -m venv venv
venv/Scripts/pip install torch torchaudio --index-url https://download.pytorch.org/whl/cu124
venv/Scripts/pip install kokoro soundfile imageio-ffmpeg numpy omegaconf

venv/Scripts/python tools/narrate.py                 # record whatever is missing, both languages
venv/Scripts/python tools/narrate.py --lang ru --prune  # also drop recordings of removed lines
venv/Scripts/python tools/narrate.py --sample out/    # compare voices on one line
```

Voices and pace can be changed with `SERENE_RU_SPEAKER` (Silero: xenia, baya, kseniya, aidar,
eugene), `SERENE_EN_VOICE` (Kokoro: af_heart, bf_emma…), `SERENE_EN_SPEED`, `SERENE_TEMPO`
(pitch-preserving slow-down) and `SERENE_BREATH_MS` (silence between sentences). After changing a
voice, delete the folder of that language and record again. A CUDA GPU makes a full run take
minutes; a CPU works too, just slower.

Licenses: Silero models are CC BY-NC-SA 4.0 (non-commercial), Kokoro-82M is Apache 2.0.
