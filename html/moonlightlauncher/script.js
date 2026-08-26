const REPO_OWNER = 'Semin696';
const REPO_NAME = 'moonlightlauncher';
const FOLDER = 'realise';
const EXE_EXT = /\.(exe|zip|jar|msi)$/i;

const API_URL = `https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/contents/${FOLDER}`;

const downloadList = document.getElementById('downloadList');
const errorState = document.getElementById('errorState');

function formatSize(bytes) {
  if (bytes == null) return '';
  const mb = bytes / 1024 / 1024;
  return mb >= 1 ? `${mb.toFixed(1)} МБ` : `${(bytes / 1024).toFixed(0)} КБ`;
}

async function loadDownloads() {
  try {
    const res = await fetch(`${API_URL}?t=${Date.now()}`);
    if (!res.ok) throw new Error(res.status);
    const files = await res.json();

    if (!Array.isArray(files)) throw new Error('bad response');

    const builds = files.filter(f => f.type === 'file' && EXE_EXT.test(f.name));

    if (builds.length === 0) {
      showError();
      return;
    }

    downloadList.innerHTML = builds.map(file => `
      <div class="download-item">
        <div class="file-info">
          <div class="file-name">${file.name}</div>
          <div class="file-meta">Размер: ${formatSize(file.size)}</div>
        </div>
        <a class="btn primary" href="${file.download_url}" download>Скачать</a>
      </div>
    `).join('');
  } catch {
    showError();
  }
}

function showError() {
  downloadList.classList.add('hidden');
  errorState.classList.remove('hidden');
}

loadDownloads();
