// Strawberrycandy Global Cloud Archive - Dedicated Backend Server
// Zero external dependencies required - runs natively on Node.js 18+

const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = process.env.PORT || 3001;
const NOVELS_FILE = path.join(__dirname, '..', 'novels.json');
const DATA_DIR = path.join(__dirname, 'data');
const COMMENTS_FILE = path.join(DATA_DIR, 'comments.json');
const SLOTS_FILE = path.join(DATA_DIR, 'slots.json');

// Ensure data directory exists
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

// Helpers for reading/writing JSON files safely
function readJsonFile(filePath, fallback = []) {
  try {
    if (fs.existsSync(filePath)) {
      const content = fs.readFileSync(filePath, 'utf8').trim();
      return content ? JSON.parse(content) : fallback;
    }
  } catch (err) {
    console.error(`Error reading ${filePath}:`, err.message);
  }
  return fallback;
}

function writeJsonFile(filePath, data) {
  try {
    fs.writeFileSync(filePath, JSON.stringify(data, null, 2), 'utf8');
    return true;
  } catch (err) {
    console.error(`Error writing ${filePath}:`, err.message);
    return false;
  }
}

// Ensure files exist
if (!fs.existsSync(NOVELS_FILE)) {
  writeJsonFile(NOVELS_FILE, []);
}
if (!fs.existsSync(COMMENTS_FILE)) {
  writeJsonFile(COMMENTS_FILE, []);
}
if (!fs.existsSync(SLOTS_FILE)) {
  writeJsonFile(SLOTS_FILE, []);
}

const server = http.createServer((req, res) => {
  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, User-Agent');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  const url = new URL(req.url, `http://${req.headers.host}`);
  const pathname = url.pathname;

  // Root or Health Check
  if (pathname === '/' || pathname === '/api/status' || pathname === '/api/health') {
    const novels = readJsonFile(NOVELS_FILE, []);
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'online',
      service: 'Strawberrycandy Novel Archive Backend Server',
      version: '1.0.0',
      timestamp: Date.now(),
      totalNovels: novels.length,
      endpoints: [
        'GET /api/novels',
        'POST /api/novels',
        'PUT /api/novels',
        'DELETE /api/novels/:id',
        'GET /api/comments',
        'POST /api/comments',
        'GET /api/author-slots',
        'PUT /api/author-slots'
      ]
    }));
    return;
  }

  // --- NOVELS ENDPOINTS ---
  if (pathname === '/api/novels' || pathname === '/novels.json') {
    if (req.method === 'GET') {
      const novels = readJsonFile(NOVELS_FILE, []);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(novels));
      return;
    }

    if (req.method === 'POST' || req.method === 'PUT') {
      let body = '';
      req.on('data', chunk => {
        body += chunk;
        if (body.length > 50 * 1024 * 1024) { // 50 MB limit
          req.destroy();
        }
      });

      req.on('end', () => {
        try {
          const parsed = JSON.parse(body);
          const currentNovels = readJsonFile(NOVELS_FILE, []);
          const novelMap = new Map();

          currentNovels.forEach(n => {
            if (n && n.id) novelMap.set(n.id, n);
          });

          if (Array.isArray(parsed)) {
            parsed.forEach(n => {
              if (n && n.id) novelMap.set(n.id, n);
            });
          } else if (parsed && typeof parsed === 'object') {
            const novelItem = parsed.novel || parsed.data || parsed;
            if (novelItem && novelItem.id) {
              novelMap.set(novelItem.id, novelItem);
            } else if (Array.isArray(parsed.novels)) {
              parsed.novels.forEach(n => {
                if (n && n.id) novelMap.set(n.id, n);
              });
            }
          }

          const updatedList = Array.from(novelMap.values());
          writeJsonFile(NOVELS_FILE, updatedList);

          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({
            success: true,
            message: 'Novel published successfully to Strawberrycandy Backend Server',
            totalNovels: updatedList.length
          }));
        } catch (err) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: false, error: err.message }));
        }
      });
      return;
    }
  }

  // Single Novel Delete / Get
  if (pathname.startsWith('/api/novels/')) {
    const novelId = pathname.substring('/api/novels/'.length).trim();
    if (req.method === 'DELETE') {
      const currentNovels = readJsonFile(NOVELS_FILE, []);
      const filtered = currentNovels.filter(n => n.id !== novelId);
      writeJsonFile(NOVELS_FILE, filtered);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ success: true, remaining: filtered.length }));
      return;
    }
    if (req.method === 'GET') {
      const currentNovels = readJsonFile(NOVELS_FILE, []);
      const novel = currentNovels.find(n => n.id === novelId);
      if (novel) {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify(novel));
      } else {
        res.writeHead(404, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: 'Novel not found' }));
      }
      return;
    }
  }

  // --- COMMENTS ENDPOINTS ---
  if (pathname === '/api/comments') {
    if (req.method === 'GET') {
      const comments = readJsonFile(COMMENTS_FILE, []);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(comments));
      return;
    }

    if (req.method === 'POST') {
      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const newComment = JSON.parse(body);
          const comments = readJsonFile(COMMENTS_FILE, []);
          comments.unshift(newComment);
          writeJsonFile(COMMENTS_FILE, comments);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: true, comment: newComment }));
        } catch (err) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: err.message }));
        }
      });
      return;
    }
  }

  // --- AUTHOR SLOTS ENDPOINTS ---
  if (pathname === '/api/author-slots') {
    if (req.method === 'GET') {
      const slots = readJsonFile(SLOTS_FILE, []);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(slots));
      return;
    }

    if (req.method === 'POST' || req.method === 'PUT') {
      let body = '';
      req.on('data', chunk => { body += chunk; });
      req.on('end', () => {
        try {
          const incoming = JSON.parse(body);
          const slotsList = Array.isArray(incoming) ? incoming : (incoming.slots || [incoming]);
          writeJsonFile(SLOTS_FILE, slotsList);
          res.writeHead(200, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ success: true, count: slotsList.length }));
        } catch (err) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: err.message }));
        }
      });
      return;
    }
  }

  // 404
  res.writeHead(404, { 'Content-Type': 'application/json' });
  res.end(JSON.stringify({ error: 'Endpoint not found' }));
});

server.listen(PORT, () => {
  console.log(`Strawberrycandy Backend Server running on http://localhost:${PORT}`);
});
