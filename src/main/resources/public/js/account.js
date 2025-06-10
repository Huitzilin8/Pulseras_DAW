// File: src/main/resources/public/account.html
document.addEventListener('DOMContentLoaded', () => {
    const favoritesList = document.getElementById('favoritesList');
    const buildsList = document.getElementById('buildsList');
    const noFavoritesMessage = document.getElementById('noFavoritesMessage');
    const noBuildsMessage = document.getElementById('noBuildsMessage');
    const favoritesSpinner = document.getElementById('favoritesSpinner');
    const buildsSpinner = document.getElementById('buildsSpinner');

    // Function to display a bracelet item
    function renderBracelet(bracelet, listElement, type = 'favorite') {
        const itemHtml = `
            <div class="bracelet-item" data-id="${bracelet._id}">
                <div>
                    <img src="${bracelet.imageUrl || 'https://via.placeholder.com/60'}" alt="${bracelet.name}" class="bracelet-image">
                    <span>${bracelet.name || 'Pulsera sin nombre'}</span>
                </div>
                ${type === 'favorite' ? `<button class="btn btn-danger btn-sm remove-favorite" data-id="${bracelet._id}">
                    <i class="bi bi-trash"></i> Quitar
                </button>` : ''}
            </div>
        `;
        listElement.insertAdjacentHTML('beforeend', itemHtml);
    }

    // --- Load Favorite Bracelets ---
    async function loadFavorites() {
        favoritesSpinner.classList.remove('d-none');
        favoritesList.innerHTML = ''; // Clear previous items
        noFavoritesMessage.classList.add('d-none');

        try {
            const response = await fetch('/api/profile/favoritos');
            if (!response.ok) {
                // If not authenticated, server returns 401 and `halt()` which closes connection
                // Or if it's another server error, throw it.
                // Assuming your AuthController redirects to login on 401 if it detects unauthenticated.
                // If you get here with a 401, it means the /api/profile/favoritos endpoint itself threw it.
                const errorData = await response.json();
                console.error('Error al cargar favoritos:', errorData.error);
                alert('No autorizado: por favor inicia sesión para ver tus favoritos.');
                // Optionally redirect to login page if this isn't handled by AuthController's filter
                window.location.href = '/login.html';
                return;
            }
            const favorites = await response.json();

            if (favorites.length === 0) {
                noFavoritesMessage.classList.remove('d-none');
            } else {
                favorites.forEach(bracelet => renderBracelet(bracelet, favoritesList, 'favorite'));
            }
        } catch (error) {
            console.error('Error de conexión o inesperado al cargar favoritos:', error);
            alert('Hubo un problema al cargar tus pulseras favoritas. Intenta de nuevo más tarde.');
        } finally {
            favoritesSpinner.classList.add('d-none');
        }
    }

    // --- Load Custom Built Bracelets ---
    async function loadBuilds() {
        buildsSpinner.classList.remove('d-none');
        buildsList.innerHTML = ''; // Clear previous items
        noBuildsMessage.classList.add('d-none');

        try {
            const response = await fetch('/api/profile/builds');
            if (!response.ok) {
                const errorData = await response.json();
                console.error('Error al cargar diseños:', errorData.error);
                alert('No autorizado: por favor inicia sesión para ver tus diseños.');
                window.location.href = '/login.html';
                return;
            }
            const builds = await response.json();

            if (builds.length === 0) {
                noBuildsMessage.classList.remove('d-none');
            } else {
                builds.forEach(bracelet => renderBracelet(bracelet, buildsList, 'build'));
            }
        } catch (error) {
            console.error('Error de conexión o inesperado al cargar diseños:', error);
            alert('Hubo un problema al cargar tus diseños personalizados. Intenta de nuevo más tarde.');
        } finally {
            buildsSpinner.classList.add('d-none');
        }
    }

    // --- Add to Favorites (Example function, would be called from elsewhere) ---
    // You would typically call this from a product page, not from the account page directly
    async function addFavorite(pulseraId) {
        try {
            const response = await fetch('/api/profile/favoritos', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ pulseraId })
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || 'Failed to add to favorites');
            }

            const data = await response.json();
            alert(data.message);
            loadFavorites(); // Reload the list to show the new favorite
        } catch (error) {
            console.error('Error adding to favorites:', error);
            alert('Error al agregar a favoritos: ' + error.message);
        }
    }

    // --- Remove from Favorites ---
    favoritesList.addEventListener('click', async (e) => {
        if (e.target.classList.contains('remove-favorite') || e.target.closest('.remove-favorite')) {
            const button = e.target.closest('.remove-favorite');
            const pulseraId = button.dataset.id;
            if (!confirm('¿Estás seguro de que quieres eliminar esta pulsera de tus favoritos?')) {
                return;
            }

            try {
                const response = await fetch(`/api/profile/favoritos/${pulseraId}`, {
                    method: 'DELETE'
                });

                if (response.status === 204) { // 204 No Content for successful DELETE
                    alert('Pulsera eliminada de favoritos.');
                    loadFavorites(); // Reload the list to reflect the change
                } else if (!response.ok) {
                    const errorData = await response.json();
                    throw new Error(errorData.error || 'Failed to remove from favorites');
                }
            } catch (error) {
                console.error('Error removing from favorites:', error);
                alert('Error al eliminar de favoritos: ' + error.message);
            }
        }
    });

    // Initial load of favorites and builds when the page loads
    loadFavorites();
    loadBuilds();
});
