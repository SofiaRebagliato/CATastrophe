/**
 * CATastrophe — Frontend utilities
 */

// ── Estado de sesión y gato activo ──
const App = {
    user: null,
    activeCat: null,
    _catNameCache: {}, // { catId: { name, avatarUrl, humanId } }
    _notifInterval: null,

    async checkSession() {
        try {
            const res = await fetch('/api/v1/auth/me');
            if (res.ok) {
                this.user = await res.json();
                return true;
            }
        } catch (e) { /* no session */ }
        this.user = null;
        this.activeCat = null;
        return false;
    },

    async loadActiveCat() {
        if (!this.user) return;
        try {
            const res = await fetch('/api/v1/cats/mine');
            if (res.ok) {
                const cats = await res.json();
                if (cats.length > 0) {
                    const savedId = localStorage.getItem('activeCatId');
                    this.activeCat = cats.find(c => c.id === savedId) || cats[0];
                }
            }
        } catch (e) { /* ignore */ }
    },

    setActiveCat(cat) {
        this.activeCat = cat;
        localStorage.setItem('activeCatId', cat.id);
        this.refreshNav();
        App.toast(`Ahora actúas como ${cat.name} 🐱`, 'info');
    },

    getHeaders() {
        const headers = { 'Content-Type': 'application/json' };
        if (this.activeCat) {
            headers['X-Cat-Id'] = this.activeCat.id;
        }
        return headers;
    },

    getCatHeaders() {
        const headers = {};
        if (this.activeCat) {
            headers['X-Cat-Id'] = this.activeCat.id;
        }
        return headers;
    },

    // ── Resolución de nombres de gatos ──
    async resolveCatNames(catIds) {
        const unknown = [...new Set(catIds)].filter(id => id && !this._catNameCache[id]);
        if (unknown.length === 0) return;
        try {
            const res = await fetch(`/api/v1/cats/batch?ids=${unknown.join('&ids=')}`);
            if (res.ok) {
                const summaries = await res.json();
                summaries.forEach(s => {
                    this._catNameCache[s.id] = { name: s.name, avatarUrl: s.avatarUrl, humanId: s.humanId };
                });
                // Also resolve human names for display
                const humanIds = [...new Set(summaries.map(s => s.humanId).filter(id => id && !this._humanNameCache?.[id]))];
                if (humanIds.length > 0) {
                    await this._resolveHumanNames(humanIds);
                }
            }
        } catch (e) { /* ignore */ }
    },

    _humanNameCache: {},

    async _resolveHumanNames(humanIds) {
        for (const hId of humanIds) {
            try {
                const res = await fetch(`/api/v1/humans/${hId}`);
                if (res.ok) {
                    const human = await res.json();
                    this._humanNameCache[hId] = human.displayName || human.username;
                }
            } catch (e) { /* ignore */ }
        }
    },

    getCatDisplayName(catId) {
        const cached = this._catNameCache[catId];
        if (!cached) return catId ? catId.substring(0, 8) + '...' : '???';
        const humanName = this._humanNameCache?.[cached.humanId];
        return humanName ? `${cached.name} - ${humanName}` : cached.name;
    },

    getCatName(catId) {
        const cached = this._catNameCache[catId];
        return cached ? cached.name : (catId ? catId.substring(0, 8) + '...' : '???');
    },

    getCatAvatar(catId) {
        const cached = this._catNameCache[catId];
        return cached?.avatarUrl || null;
    },

    // ── Notificaciones polling ──
    startNotifPolling() {
        this.stopNotifPolling();
        this._updateNotifBadge();
        this._notifInterval = setInterval(() => this._updateNotifBadge(), 10000);
    },

    stopNotifPolling() {
        if (this._notifInterval) {
            clearInterval(this._notifInterval);
            this._notifInterval = null;
        }
    },

    async _updateNotifBadge() {
        if (!this.activeCat) return;
        try {
            const [notifRes, msgRes] = await Promise.all([
                fetch('/api/v1/notifications/count', { headers: this.getCatHeaders() }),
                fetch('/api/v1/messages/unread', { headers: this.getCatHeaders() })
            ]);
            let notifCount = 0, msgCount = 0;
            if (notifRes.ok) { const d = await notifRes.json(); notifCount = d.unreadCount || 0; }
            if (msgRes.ok) { const d = await msgRes.json(); msgCount = d.unreadCount || 0; }

            const notifBadge = document.getElementById('notif-badge');
            const msgBadge = document.getElementById('msg-badge');
            if (notifBadge) {
                notifBadge.textContent = notifCount > 0 ? notifCount : '';
                notifBadge.classList.toggle('hidden', notifCount === 0);
            }
            if (msgBadge) {
                msgBadge.textContent = msgCount > 0 ? msgCount : '';
                msgBadge.classList.toggle('hidden', msgCount === 0);
            }
        } catch (e) { /* ignore */ }
    },

    async refreshNav() {
        const nav = document.getElementById('nav-user');
        if (this.user) {
            nav.innerHTML = `
                <div class="flex items-center space-x-3">
                    ${this.activeCat ? `<span class="text-xs bg-cat-100 text-cat-700 px-2 py-1 rounded-full font-medium">${this.activeCat.name}</span>` : ''}
                    <span class="text-sm text-gray-700">🐾 <strong>${this.user.displayName || this.user.username}</strong></span>
                    <button onclick="App.showDashboard()" class="text-sm text-gray-500 hover:text-cat-600 transition" title="Panel">🏠</button>
                    <button onclick="App.showMessages()" class="relative text-sm text-gray-500 hover:text-cat-600 transition" title="Mensajes">
                        💬<span id="msg-badge" class="hidden absolute -top-1 -right-2 bg-red-500 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center"></span>
                    </button>
                    <button onclick="App.showNotifications()" class="relative text-sm text-gray-500 hover:text-cat-600 transition" title="Notificaciones">
                        🔔<span id="notif-badge" class="hidden absolute -top-1 -right-2 bg-red-500 text-white text-xs rounded-full w-4 h-4 flex items-center justify-center"></span>
                    </button>
                    <button onclick="App.logout()"
                            class="text-sm bg-cat-100 text-cat-700 px-3 py-1.5 rounded-lg hover:bg-cat-200 transition">
                        Salir
                    </button>
                </div>`;
            this.startNotifPolling();
        } else {
            nav.innerHTML = `
                <div class="flex items-center space-x-2">
                    <button onclick="App.showLogin()"
                            class="text-sm bg-cat-600 text-white px-4 py-1.5 rounded-lg hover:bg-cat-700 transition">
                        Iniciar sesión
                    </button>
                    <button onclick="App.showRegister()"
                            class="text-sm bg-white border border-cat-300 text-cat-700 px-4 py-1.5 rounded-lg hover:bg-cat-50 transition">
                        Registro
                    </button>
                </div>`;
            this.stopNotifPolling();
        }
    },

    // ── Navegación ──
    showLogin() {
        htmx.ajax('GET', '/pages/login.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showRegister() {
        htmx.ajax('GET', '/pages/register.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showDashboard() {
        htmx.ajax('GET', '/pages/dashboard.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showHome() {
        htmx.ajax('GET', '/pages/home.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showFeed() {
        htmx.ajax('GET', '/pages/feed.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showCatProfile(catId) {
        window._catProfileId = catId;
        htmx.ajax('GET', '/pages/cat-profile.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showMessages() {
        htmx.ajax('GET', '/pages/messages.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showConversation(catId) {
        window._conversationCatId = catId;
        htmx.ajax('GET', '/pages/conversation.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showAdventures() {
        htmx.ajax('GET', '/pages/adventures.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showChallenges() {
        htmx.ajax('GET', '/pages/challenges.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showRankings() {
        htmx.ajax('GET', '/pages/rankings.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showNotifications() {
        htmx.ajax('GET', '/pages/notifications.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showPersonality() {
        htmx.ajax('GET', '/pages/personality.html', { target: '#main-content', swap: 'innerHTML' });
    },
    showBadges() {
        htmx.ajax('GET', '/pages/badges.html', { target: '#main-content', swap: 'innerHTML' });
    },

    async logout() {
        await fetch('/api/v1/auth/logout', { method: 'POST' });
        this.user = null;
        this.activeCat = null;
        localStorage.removeItem('activeCatId');
        this.stopNotifPolling();
        this.refreshNav();
        this.showHome();
        App.toast('Sesión cerrada. Tus gatos te echarán de menos. 🐱', 'info');
    },

    toast(message, type = 'success') {
        const container = document.getElementById('toast-container');
        const colors = {
            success: 'bg-green-500',
            error: 'bg-red-500',
            info: 'bg-cat-500'
        };
        const toast = document.createElement('div');
        toast.className = `${colors[type]} text-white px-4 py-3 rounded-lg shadow-lg fade-in text-sm max-w-sm`;
        toast.textContent = message;
        container.appendChild(toast);
        setTimeout(() => toast.remove(), 4000);
    },

    // ── Utilidades ──
    timeAgo(dateStr) {
        if (!dateStr) return '';
        const now = new Date();
        const date = new Date(dateStr);
        const diff = Math.floor((now - date) / 1000);
        if (diff < 60) return 'ahora';
        if (diff < 3600) return `hace ${Math.floor(diff / 60)}m`;
        if (diff < 86400) return `hace ${Math.floor(diff / 3600)}h`;
        return `hace ${Math.floor(diff / 86400)}d`;
    },

    requireCat() {
        if (!this.activeCat) {
            App.toast('Primero registra un gato para poder interactuar.', 'error');
            App.showDashboard();
            return false;
        }
        return true;
    }
};

// ── Event listeners ──
document.body.addEventListener('htmx:responseError', (e) => {
    App.toast('Error al cargar la página.', 'error');
});

// Init
document.addEventListener('DOMContentLoaded', async () => {
    const loggedIn = await App.checkSession();
    if (loggedIn) {
        await App.loadActiveCat();
        App.refreshNav();
        App.showDashboard();
    } else {
        App.refreshNav();
    }
});
