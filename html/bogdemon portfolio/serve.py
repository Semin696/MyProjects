"""Локальный dev-сервер с live-reload: python serve.py [порт]

Раздаёт статику из папки сервера и вставляет в HTML скрипт,
который следит за /__reload и перезагружает страницу при изменении файлов.
"""
import hashlib
import http.server
import os
import sys
import threading
import time

ROOT = os.path.dirname(os.path.abspath(__file__))
PORT = int(sys.argv[1]) if len(sys.argv) > 1 else 5500

RELOAD_JS = """
(() => {
  const poll = async () => {
    try {
      const r = await fetch('/__reload', { cache: 'no-store' });
      const v = await r.text();
      if (window.__liveV === undefined) window.__liveV = v;
      else if (v !== window.__liveV) location.reload();
    } catch {}
    setTimeout(poll, 700);
  };
  poll();
})();
"""

_sig_lock = threading.Lock()
_sig_cache = {"value": "", "at": 0.0}


def signature():
    with _sig_lock:
        now = time.time()
        if now - _sig_cache["at"] < 0.4:
            return _sig_cache["value"]
        parts = []
        for dirpath, dirnames, filenames in os.walk(ROOT):
            dirnames[:] = [d for d in dirnames if d not in (".git", "node_modules", "__pycache__")]
            for name in filenames:
                p = os.path.join(dirpath, name)
                try:
                    st = os.stat(p)
                    parts.append(f"{os.path.relpath(p, ROOT)}|{st.st_mtime_ns}|{st.st_size}")
                except OSError:
                    pass
        value = hashlib.sha1("\n".join(sorted(parts)).encode()).hexdigest()
        _sig_cache.update(value=value, at=now)
        return value


class Handler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=ROOT, **kwargs)

    def end_headers(self):
        self.send_header("Cache-Control", "no-store")
        super().end_headers()

    def copyfile(self, src, dst):
        path = self.path.split("?")[0]
        if path == "/" or path.lower().endswith(".html"):
            html = src.read().decode("utf-8", errors="replace")
            snippet = f'<script>{RELOAD_JS}</script></body>'
            html = html.replace("</body>", snippet, 1) if "</body>" in html else html + snippet
            dst.write(html.encode("utf-8"))
            return
        super().copyfile(src, dst)

    def do_GET(self):
        path = self.path.split("?")[0]
        if path == "/__reload":
            body = signature().encode()
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            return
        super().do_GET()

    def log_message(self, fmt, *args):
        sys.stderr.write("  %s\n" % (fmt % args))


class Server(http.server.ThreadingHTTPServer):
    allow_reuse_address = True


if __name__ == "__main__":
    with Server(("127.0.0.1", PORT), Handler) as httpd:
        print(f"live: http://127.0.0.1:{PORT}  (root: {ROOT})")
        httpd.serve_forever()
