document.addEventListener('click', function (event) {
    const button = event.target.closest('[data-password-toggle]');
    if (!button) {
        return;
    }

    const group = button.closest('.password-input-group');
    const input = group && group.querySelector('input');
    if (!input) {
        return;
    }

    const isHidden = input.type === 'password';
    input.type = isHidden ? 'text' : 'password';
    button.setAttribute('aria-pressed', String(isHidden));
    button.setAttribute(
            'aria-label', isHidden ? 'Masquer le mot de passe' : 'Afficher le mot de passe');
    button.setAttribute(
            'title', isHidden ? 'Masquer le mot de passe' : 'Afficher le mot de passe');

    const icon = button.querySelector('i');
    if (icon) {
        icon.classList.toggle('fa-eye', !isHidden);
        icon.classList.toggle('fa-eye-slash', isHidden);
    }
});
