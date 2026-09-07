# Strawberrycandy Dedicated Backend Server

This is the dedicated Node.js backend server for Strawberrycandy Novel Archive.

## Features
- **Zero external dependencies**: Built purely with native Node.js (`http`, `fs`, `path`).
- **Endpoints**:
  - `GET /api/novels`: Returns the list of all published novels (reads from `novels.json`).
  - `POST /api/novels`: Receives novel uploads from Translators and Owner, saving them directly into `novels.json`.
  - `DELETE /api/novels/:id`: Removes a novel from the archive.
  - `GET /api/comments` & `POST /api/comments`: Community comments and feedback storage.
  - `GET /api/author-slots` & `PUT /api/author-slots`: Translator slot profile management.
- **CORS enabled**: Supports cross-origin requests from web browsers and Android apps.

## Running Locally or in Production
```bash
npm start
# Server listens on port 3001 (or process.env.PORT)
```
