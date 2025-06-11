document.addEventListener('DOMContentLoaded', () => {
    // Elementos del DOM
    const usersTableBody = document.getElementById('usersTableBody');
    const braceletsCardContainer = document.getElementById('braceletsCardContainer');
    const materialsTableBody = document.getElementById('materialsTableBody'); // Nuevo
    const usersSpinner = document.getElementById('usersSpinner');
    const braceletsSpinner = document.getElementById('braceletsSpinner');
    const materialsSpinner = document.getElementById('materialsSpinner'); // Nuevo
    const braceletImageInput = document.getElementById('braceletImage');
    const imagePreview = document.getElementById('imagePreview');
    const imgpathInput = document.getElementById('imgpath');

    // Modales
    const userModal = new bootstrap.Modal(document.getElementById('userModal'));
    const braceletModal = new bootstrap.Modal(document.getElementById('braceletModal'));
    const materialModal = new bootstrap.Modal(document.getElementById('materialModal')); // Nuevo
    const confirmModal = new bootstrap.Modal(document.getElementById('confirmModal'));
    const myAlertModal = new bootstrap.Modal(document.getElementById('myAlertModal'));
    const myAlertModalLabel = document.getElementById('myAlertModalLabel');
    const myAlertModalBody = document.getElementById('myAlertModalBody');

    // Variables de estado
    let currentAction = '';
    let currentItemId = '';
    let currentBracelet = null; // Para almacenar los datos de la pulsera actual al editar

    // Inicialización
    checkAdminStatus();
    loadUsers();
    loadBracelets();
    loadMaterials(); // Cargar materiales al inicio

    // Event Listeners
    document.getElementById('addUserBtn').addEventListener('click', () => showUserModal('add'));
    document.getElementById('addBraceletBtn').addEventListener('click', () => showBraceletModal('add'));
    document.getElementById('addMaterialBtn').addEventListener('click', () => showMaterialModal('add')); // Nuevo
    document.getElementById('saveUserBtn').addEventListener('click', saveUser);
    document.getElementById('saveBraceletBtn').addEventListener('click', saveBracelet);
    document.getElementById('saveMaterialBtn').addEventListener('click', saveMaterial); // Nuevo
    document.getElementById('confirmActionBtn').addEventListener('click', confirmAction);

    if (braceletImageInput) {
        braceletImageInput.addEventListener('change', handleImageUpload);
    }

    // Verificar estado de administrador
    function checkAdminStatus() {
        fetch('/api/auth/status')
            .then(response => response.json())
            .then(data => {
                if (!data.authenticated || data.role !== 'admin') {
                    window.location.href = '/';
                }
            })
            .catch(error => {
                console.error('Error checking admin status:', error);
                window.location.href = '/';
            });
    }

    // Cargar usuarios
    function loadUsers() {
        usersSpinner.style.display = 'block';
        usersTableBody.innerHTML = '';

        fetch('/api/admin/usuarios')
            .then(response => {
                if (!response.ok) throw new Error('Error al cargar usuarios');
                return response.json();
            })
            .then(users => {
                users.forEach(user => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${user.id.substring(18)}</td>
                        <td>${user.nombreUsuario || 'N/A'}</td>
                        <td>${user.correo || 'N/A'}</td>
                        <td>${user.rol || 'user'}</td>
                        <td class="action-buttons">
                            <button class="btn btn-sm btn-warning edit-user" data-id="${user.id}">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-sm btn-danger delete-user" data-id="${user.id}">
                                <i class="bi bi-trash"></i>
                            </button>
                        </td>
                    `;
                    usersTableBody.appendChild(row);
                });

                // Agregar event listeners a los botones
                document.querySelectorAll('.edit-user').forEach(btn => {
                    btn.addEventListener('click', () => showUserModal('edit', btn.dataset.id));
                });

                document.querySelectorAll('.delete-user').forEach(btn => {
                    btn.addEventListener('click', () => showConfirmModal('deleteUser', btn.dataset.id, '¿Estás seguro de que deseas eliminar este usuario?'));
                });
            })
            .catch(error => {
                console.error('Error:', error);
                showAlert('danger','Error al cargar usuarios: '+ error);
            })
            .finally(() => {
                usersSpinner.style.display = 'none';
            });
    }

    // Cargar pulseras
    function loadBracelets() {
        braceletsSpinner.style.display = 'block';
        braceletsCardContainer.innerHTML = ''; // Limpiar el contenedor de cartas

        fetch('/api/public/pulseras')
            .then(response => {
                if (!response.ok) throw new Error('Error al cargar pulseras');
                return response.json();
            })
            .then(bracelets => {
                bracelets.forEach(bracelet => {
                    const card = document.createElement('div');
                    card.classList.add('col');
                    const imageUrl = bracelet.imgURL ? bracelet.imgURL : ''; // Usa la URL de la imagen si existe
                    const imageHtml = imageUrl ? `<img src="${imageUrl}" class="card-img-top" alt="${bracelet.nombre}">` : `<div class="bg-secondary text-white d-flex align-items-center justify-content-center" style="height: 200px;">Sin Imagen</div>`;

                    // Determinar el texto y color del botón basado en el estado 'delisted'
                    const actionButtonClass = bracelet.delisted ? 'btn-success' : 'btn-danger';
                    const actionButtonIcon = bracelet.delisted ? 'bi-check-circle' : 'bi-trash';
                    const actionButtonText = bracelet.delisted ? 'Reactivar' : 'Retirar';
                    const actionButtonDataAction = bracelet.delisted ? 'activateBracelet' : 'delistBracelet'; // Cambiado 'deleteBracelet' a 'delistBracelet' para mayor claridad

                    card.innerHTML = `
                        <div class="card h-100">
                            ${imageHtml}
                            <div class="card-body">
                                <h5 class="card-title">${bracelet.nombre || 'Sin nombre'}</h5>
                                <p class="card-text">Precio: $${bracelet.precio?.toFixed(2) || '0.00'}</p>
                                <p class="card-text"><small class="text-muted">${bracelet.userBuilt ? 'Personalizada' : 'Predefinida'}</small></p>
                                <p class="card-text"><small class="text-muted">${bracelet.delisted ? 'Retirada' : 'Disponible'}</small></p>
                                <div class="action-buttons">
                                    <button class="btn btn-sm btn-warning edit-bracelet" data-id="${bracelet.id}">
                                        <i class="bi bi-pencil"></i> Editar
                                    </button>
                                    <button class="btn btn-sm ${actionButtonClass} bracelet-status-toggle" data-id="${bracelet.id}" data-action="${actionButtonDataAction}">
                                        <i class="bi ${actionButtonIcon}"></i> ${actionButtonText}
                                    </button>
                                </div>
                            </div>
                        </div>
                    `;
                    braceletsCardContainer.appendChild(card);
                });

                // Agregar event listeners a los botones de las cartas
                document.querySelectorAll('.edit-bracelet').forEach(btn => {
                    btn.addEventListener('click', () => showBraceletModal('edit', btn.dataset.id));
                });

                document.querySelectorAll('.bracelet-status-toggle').forEach(btn => {
                    btn.addEventListener('click', () => {
                        const bracelet = bracelets.find(b => b.id === btn.dataset.id);
                        const action = bracelet.delisted ? 'activateBracelet' : 'delistBracelet'; // Usar 'delistBracelet' para la acción de retirar
                        const message = bracelet.delisted ?
                            '¿Estás seguro de que deseas reactivar esta pulsera?' :
                            '¿Estás seguro de que deseas retirar esta pulsera? Esto la ocultará de la vista pública.';
                        showConfirmModal(action, btn.dataset.id, message);
                    });
                });
            })
            .catch(error => {
                console.error('Error:', error);
                showAlert('danger','Error al cargar pulseras: ' + error);
            })
            .finally(() => {
                braceletsSpinner.style.display = 'none';
            });
    }

    // Cargar Materiales (NUEVA FUNCIÓN)
    function loadMaterials() {
        materialsSpinner.style.display = 'block';
        materialsTableBody.innerHTML = '';

        fetch('/api/public/materials') // Endpoint para obtener materiales
            .then(response => {
                if (!response.ok) throw new Error('Error al cargar materiales');
                return response.json();
            })
            .then(materials => {
                materials.forEach(material => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${material.id}</td>
                        <td>${material.nombre || 'N/A'}</td>
                        <td>${material.descripcion || 'N/A'}</td>
                        <td>$${material.precio?.toFixed(2) || '0.00'}</td>
                        <td>${material.cantidadInventario || 0}</td>
                        <td class="action-buttons">
                            <button class="btn btn-sm btn-warning edit-material" data-id="${material.id}">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-sm btn-danger delete-material" data-id="${material.id}">
                                <i class="bi bi-trash"></i>
                            </button>
                        </td>
                    `;
                    materialsTableBody.appendChild(row);
                });

                // Agregar event listeners a los botones
                document.querySelectorAll('.edit-material').forEach(btn => {
                    btn.addEventListener('click', () => showMaterialModal('edit', btn.dataset.id));
                });

                document.querySelectorAll('.delete-material').forEach(btn => {
                    btn.addEventListener('click', () => showConfirmModal('deleteMaterial', btn.dataset.id, '¿Estás seguro de que deseas eliminar este material?'));
                });
            })
            .catch(error => {
                console.error('Error:', error);
                showAlert('danger','Error al cargar materiales: '+ error);
            })
            .finally(() => {
                materialsSpinner.style.display = 'none';
            });
    }

    // Mostrar modal de usuario
    function showUserModal(action, userId = null) {
        const modalTitle = document.getElementById('userModalTitle');
        const passwordFields = document.getElementById('passwordFields');

        if (action === 'add') {
            modalTitle.textContent = 'Agregar Nuevo Usuario';
            document.getElementById('userForm').reset();
            passwordFields.style.display = 'block';
            document.getElementById('userPassword').required = true;
            currentAction = 'addUser';
        } else {
            modalTitle.textContent = 'Editar Usuario';
            passwordFields.style.display = 'block';
            document.getElementById('userPassword').required = false;
            currentAction = 'editUser';
            currentItemId = userId;

            // Cargar datos del usuario
            fetch(`/api/admin/usuario/${userId}`)
                .then(response => response.json())
                .then(user => {
                    document.getElementById('userId').value = user.id;
                    document.getElementById('userName').value = user.nombreUsuario;
                    document.getElementById('userEmail').value = user.correo;
                    document.getElementById('userRole').value = user.rol;
                })
                .catch(error => {
                    console.error('Error al cargar usuario:', error);
                    showAlert('danger','Error al cargar datos del usuario: ' + error);
                });
        }

        userModal.show();
    }

    // Mostrar modal de pulsera
    function showBraceletModal(action, braceletId = null) {
        const modalTitle = document.getElementById('braceletModalTitle');
        const braceletForm = document.getElementById('braceletForm');
        braceletForm.reset(); // Limpiar el formulario al abrir el modal
        imagePreview.innerHTML = ''; // Limpiar la vista previa de la imagen
        imgpathInput.value = ''; // Limpiar el path de la imagen
        currentBracelet = null; // Resetear la pulsera actual

        if (action === 'add') {
            modalTitle.textContent = 'Agregar Nueva Pulsera';
            currentAction = 'addBracelet';
        } else {
            modalTitle.textContent = 'Editar Pulsera';
            currentAction = 'editBracelet';
            currentItemId = braceletId;

            // Cargar datos de la pulsera
            fetch(`/api/admin/pulseras/${braceletId}`)
                .then(response => response.json())
                .then(bracelet => {
                    currentBracelet = bracelet; // Guardar la pulsera actual
                    document.getElementById('braceletId').value = bracelet.id;
                    document.getElementById('braceletName').value = bracelet.nombre;
                    document.getElementById('braceletDescription').value = bracelet.descripcion;
                    document.getElementById('braceletPrice').value = bracelet.precio;
                    document.getElementById('braceletStatus').value = bracelet.delisted;
                    if (bracelet.imgURL) {
                        imagePreview.innerHTML = `<img src="${bracelet.imgURL}" alt="Vista previa" style="max-width: 100px; max-height: 100px;">`;
                        // Guardar solo el nombre del archivo si la URL es /img/{path}.png
                        const imgPathMatch = bracelet.imgURL.match(/\/img\/(.*)/);
                        if (imgPathMatch && imgPathMatch[1]) {
                            imgpathInput.value = imgPathMatch[1];
                        }
                    }
                })
                .catch(error => {
                    console.error('Error al cargar pulsera:', error);
                    showAlert('danger','Error al cargar datos de la pulsera: ' + error);
                });
        }

        braceletModal.show();
    }

    // Mostrar modal de material (NUEVA FUNCIÓN)
    function showMaterialModal(action, materialId = null) {
        const modalTitle = document.getElementById('materialModalTitle');
        document.getElementById('materialForm').reset(); // Limpiar el formulario

        if (action === 'add') {
            modalTitle.textContent = 'Agregar Nuevo Material';
            currentAction = 'addMaterial';
        } else {
            modalTitle.textContent = 'Editar Material';
            currentAction = 'editMaterial';
            currentItemId = materialId;

            // Cargar datos del material
            fetch(`/api/admin/materials/${materialId}`)
                .then(response => response.json())
                .then(material => {
                    document.getElementById('materialId').value = material.id;
                    document.getElementById('materialName').value = material.nombre;
                    document.getElementById('materialDescription').value = material.descripcion;
                    document.getElementById('materialPrice').value = material.precio;
                    document.getElementById('materialInventory').value = material.cantidadInventario;
                })
                .catch(error => {
                    console.error('Error al cargar material:', error);
                    showAlert('danger','Error al cargar datos del material: ' + error);
                });
        }

        materialModal.show();
    }

    // Mostrar modal de confirmación
    function showConfirmModal(action, itemId, message) {
        currentAction = action;
        currentItemId = itemId;
        document.getElementById('confirmModalBody').textContent = message;
        confirmModal.show();
    }

    // Guardar usuario
    function saveUser() {
        const userId = document.getElementById('userId').value;
        const userData = {
            nombreUsuario: document.getElementById('userName').value,
            correo: document.getElementById('userEmail').value,
            rol: document.getElementById('userRole').value
        };

        const password = document.getElementById('userPassword').value;
        if (password) {
            userData.password = password;
        }

        const url = currentAction === 'addUser' ? '/api/admin/usuario' : `/api/admin/usuario/${userId}`;
        const method = currentAction === 'addUser' ? 'POST' : 'PUT';

        fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(userData)
        })
        .then(response => {
            if (!response.ok) throw new Error('Error al guardar usuario');
            return response.json();
        })
        .then(() => {
            showAlert('success','Usuario guardado correctamente');
            userModal.hide();
            loadUsers();
        })
        .catch(error => {
            console.error('Error:', error);
            showAlert('danger','Error al guardar usuario: ' + error);
        });
    }

    // Subir imagen y obtener la ruta
    async function uploadImage(file) {
        const formData = new FormData();
        formData.append('image', file);

        try {
            const response = await fetch('/api/admin/upload/img', {
                method: 'POST',
                body: formData
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.details || 'Error al subir la imagen');
            }

            const data = await response.json();
            return data.filename; // Suponiendo que la respuesta JSON tiene un campo 'filename'
        } catch (error) {
            console.error('Error al subir la imagen:', error);
            showAlert('danger', 'Error al subir la imagen: ' + error.message);
            return null;
        }
    }

    async function handleImageUpload(event) {
        const file = event.target.files?.[0];
        if (file) {
            if (file.type === 'image/png') {
                const filename = await uploadImage(file);
                if (filename) {
                    imgpathInput.value = filename; // Guarda solo el nombre del archivo
                    imagePreview.innerHTML = `<img src="/api/public/img/${filename}" alt="Vista previa" style="max-width: 100px; max-height: 100px;">`;
                }
            } else {
                showAlert('warning', 'Por favor, selecciona un archivo de imagen PNG.');
                braceletImageInput.value = ''; // Limpiar el input
                imagePreview.innerHTML = '';
                imgpathInput.value = '';
            }
        }
    }

    // Guardar pulsera
    async function saveBracelet() {
        const braceletId = document.getElementById('braceletId').value;
        const braceletData = {
            nombre: document.getElementById('braceletName').value,
            descripcion: document.getElementById('braceletDescription').value,
            precio: parseFloat(document.getElementById('braceletPrice').value),
            delisted: document.getElementById('braceletStatus').value === 'true',
            // Si imgpathInput tiene un valor, se usa /img/{valor}. Si no, se usa la imgURL existente (si es edición) o null (si es nueva sin imagen)
            imgURL: imgpathInput.value ? `/api/public/img/${imgpathInput.value}` : (currentBracelet ? currentBracelet.imgURL : null)
        };

        const url = currentAction === 'addBracelet' ? '/api/admin/pulseras' : `/api/admin/pulseras/${braceletId}`;
        const method = currentAction === 'addBracelet' ? 'POST' : 'PUT';

        try {
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(braceletData)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || 'Error al guardar pulsera');
            }

            showAlert('success','Pulsera guardada correctamente');
            braceletModal.hide();
            loadBracelets();

        } catch (error) {
            console.error('Error:', error);
            showAlert('danger','Error al guardar pulsera: ' + error.message);
        }
    }

    // Guardar Material (NUEVA FUNCIÓN)
    async function saveMaterial() {
        const materialId = document.getElementById('materialId').value;
        const materialData = {
            nombre: document.getElementById('materialName').value,
            descripcion: document.getElementById('materialDescription').value,
            tamanoMm: parseFloat(document.getElementById('materialSize').value),
            cantidadInventario: parseInt(document.getElementById('materialInventory').value, 10)
        };

        const url = currentAction === 'addMaterial' ? '/api/admin/materials' : `/api/admin/materials/${materialId}`;
        const method = currentAction === 'addMaterial' ? 'POST' : 'PUT';

        try {
            const response = await fetch(url, {
                method: method,
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(materialData)
            });

            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || 'Error al guardar material');
            }

            showAlert('success', 'Material guardado correctamente');
            materialModal.hide();
            loadMaterials();
        } catch (error) {
            console.error('Error:', error);
            showAlert('danger', 'Error al guardar material: ' + error.message);
        }
    }

    // Confirmar acción (eliminar/activar/retirar)
    function confirmAction() {
        confirmModal.hide();

        let url, method;
        let actionType = ''; // Para saber si se afectó a usuarios, pulseras o materiales
        let bodyData = null; // Para el cuerpo de la solicitud PUT

        switch(currentAction) {
            case 'deleteUser':
                url = `/api/admin/usuario/${currentItemId}`;
                method = 'DELETE';
                actionType = 'user';
                break;

            case 'delistBracelet': // Ahora se usa para retirar
                url = `/api/admin/pulseras/${currentItemId}`;
                method = 'PUT'; // Se cambia a PUT para actualizar 'delisted'
                actionType = 'bracelet';
                bodyData = { delisted: true }; // Enviar solo el estado 'delisted'
                break;

            case 'activateBracelet': // Ahora se usa para reactivar
                url = `/api/admin/pulseras/${currentItemId}`;
                method = 'PUT'; // Se cambia a PUT para actualizar 'delisted'
                actionType = 'bracelet';
                bodyData = { delisted: false }; // Enviar solo el estado 'delisted'
                break;

            case 'deleteMaterial': // Nueva acción para eliminar material
                url = `/api/admin/materials/${currentItemId}`;
                method = 'DELETE';
                actionType = 'material';
                break;

            default:
                return;
        }

        fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: bodyData ? JSON.stringify(bodyData) : null
        })
            .then(response => {
                if (!response.ok) {
                    return response.json().then(errorData => {
                        throw new Error(errorData.error || 'Error al realizar la acción');
                    });
                }
                const contentType = response.headers.get("content-type");
                if (contentType && contentType.indexOf("application/json") !== -1) {
                    return response.json();
                } else {
                    return {};
                }
            })
            .then(() => {
                showAlert('success','Acción realizada correctamente');
                if (actionType === 'user') {
                    loadUsers();
                } else if (actionType === 'bracelet') {
                    loadBracelets();
                } else if (actionType === 'material') { // Recargar materiales
                    loadMaterials();
                }
            })
            .catch(error => {
                console.error('Error:', error);
                showAlert('danger','Error al realizar la acción: ' + error.message);
            });
    }

    /**
     * Displays a Bootstrap modal alert.
     * @param {string} type - The type of alert (e.g., 'success', 'danger', 'info', 'warning'). This affects the modal title.
     * @param {string} message - The message to display in the alert.
     */
    function showAlert(type, message, duration = 3000) {
        // Set modal title based on type
        switch (type) {
            case 'success':
                myAlertModalLabel.textContent = 'Éxito';
                myAlertModalLabel.parentElement.className = 'modal-header bg-success text-white';
                break;
            case 'danger':
                myAlertModalLabel.textContent = 'Error';
                myAlertModalLabel.parentElement.className = 'modal-header bg-danger text-white';
                break;
            case 'info':
                myAlertModalLabel.textContent = 'Información';
                myAlertModalLabel.parentElement.className = 'modal-header bg-info text-white';
                break;
            case 'warning':
                myAlertModalLabel.textContent = 'Advertencia';
                myAlertModalLabel.parentElement.className = 'modal-header bg-warning text-dark';
                break;
            default:
                myAlertModalLabel.textContent = 'Alerta';
                myAlertModalLabel.parentElement.className = 'modal-header';
        }

        // Set modal body message
        myAlertModalBody.textContent = message;

        // Show the modal
        myAlertModal.show();

        // Set timeout to hide the modal after duration (default: 3000ms = 3 seconds)
        setTimeout(() => {
            myAlertModal.hide();
        }, duration);
    }
});