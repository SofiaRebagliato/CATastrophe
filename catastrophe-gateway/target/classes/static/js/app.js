/**
 * CATastrophe — Frontend utilities
 */

// ── Estado de sesión ──
const App = {
    user: null,

    async checkSession() {
        try {
            const res = await fetch('/api/v1/auth/me');
            if (res.ok) {
                this.user = await res.json();
                return true;
            }
        } catch (e) { /* no session */ }
        this.user = null;
        return false;
    },

    async refreshNav() {
        const nav = document.getElementById('nav-user');
        if (this.user) {
            nav.innerHTML = `
                <div class="flex items-center space-x-4">
                    <span class="text-sm text-gray-700">🐾 <strong>${this.user.displayName || this.user.username}</strong></span>
                    <button onclick="App.logout()"
                            class="text-sm bg-cat-100 text-cat-700 px-3 py-1.5 rounded-lg hover:bg-cat-200 transition">
                        Salir
                    </button>
                </div>`;
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
        }
    },

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

    async logout() {
        await fetch('/api/v1/auth/logout', { method: 'POST' });
        this.user = null;
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
    }
};

// ── Event listeners para HTMX ──

// Después de login exitoso
document.body.addEventListener('loginSuccess', async (e) => {
    await App.checkSession();
    App.refreshNav();
    App.showDashboard();
    App.toast('¡Bienvenido de vuelta! Tu gato te estaba esperando. 🐱');
});

// Después de registro exitoso
document.body.addEventListener('registerSuccess', (e) => {
    App.showLogin();
    App.toast('¡Registro exitoso! Ahora inicia sesión.', 'success');
});

// Manejar errores de HTMX
document.body.addEventListener('htmx:responseError', (e) => {
    const status = e.detail.xhr.status;
    if (status === 401) {
        App.toast('Credenciales incorrectas', 'error');
    } else if (status === 409) {
        App.toast('El usuario ya existe', 'error');
    } else if (status === 400) {
        App.toast('Datos inválidos. Revisa el formulario.', 'error');
    } else {
        App.toast('Error del servidor. Intenta de nuevo.', 'error');
    }
});

// Init
document.addEventListener('DOMContentLoaded', async () => {
    const loggedIn = await App.checkSession();
    App.refreshNav();
    if (loggedIn) {
        App.showDashboard();
    }
});
