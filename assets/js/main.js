/* ------------------------------------------------------------------
   pre re — interactions
   - sticky header state + mobile menu
   - scroll reveal (respects prefers-reduced-motion)
   - waitlist forms with validation + local persistence
   ------------------------------------------------------------------ */

(function () {
  'use strict';

  var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

  /* ---------- header ---------- */
  var header = document.getElementById('siteHeader');
  var navToggle = document.getElementById('navToggle');

  function onScroll() {
    if (!header) return;
    header.classList.toggle('is-stuck', window.scrollY > 8);
  }
  onScroll();
  window.addEventListener('scroll', onScroll, { passive: true });

  if (navToggle && header) {
    navToggle.addEventListener('click', function () {
      var open = header.classList.toggle('menu-open');
      navToggle.setAttribute('aria-expanded', String(open));
    });

    // close the mobile menu after picking a destination
    header.querySelectorAll('.nav a').forEach(function (link) {
      link.addEventListener('click', function () {
        header.classList.remove('menu-open');
        navToggle.setAttribute('aria-expanded', 'false');
      });
    });

    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape' && header.classList.contains('menu-open')) {
        header.classList.remove('menu-open');
        navToggle.setAttribute('aria-expanded', 'false');
        navToggle.focus();
      }
    });
  }

  /* ---------- reveal on scroll ---------- */
  var revealables = document.querySelectorAll('.reveal');

  if (reduceMotion || !('IntersectionObserver' in window)) {
    revealables.forEach(function (el) { el.classList.add('is-in'); });
  } else {
    var observer = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (!entry.isIntersecting) return;
        var el = entry.target;
        var delay = Number(el.dataset.delay || 0);
        setTimeout(function () { el.classList.add('is-in'); }, delay);
        observer.unobserve(el);
      });
    }, { rootMargin: '0px 0px -8% 0px', threshold: 0.08 });

    revealables.forEach(function (el, i) {
      // stagger siblings a touch so grids cascade instead of popping at once
      if (!el.dataset.delay && el.parentElement) {
        var siblings = Array.prototype.filter.call(el.parentElement.children, function (c) {
          return c.classList && c.classList.contains('reveal');
        });
        if (siblings.length > 1) el.dataset.delay = String(siblings.indexOf(el) * 70);
      }
      observer.observe(el);
    });
  }

  /* ---------- waitlist forms ---------- */
  var STORAGE_KEY = 'prere.waitlist';

  function readList() {
    try { return JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]'); }
    catch (e) { return []; }
  }

  function saveEmail(email) {
    try {
      var list = readList();
      if (list.indexOf(email) === -1) list.push(email);
      localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    } catch (e) { /* private mode — the confirmation still matters */ }
  }

  function isValidEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[a-z]{2,}$/i.test(value.trim());
  }

  document.querySelectorAll('form.access-form').forEach(function (form) {
    var input = form.querySelector('input[type="email"]');
    var status = form.parentElement.querySelector('.form-status');
    var button = form.querySelector('button[type="submit"]');
    var originalLabel = button ? button.innerHTML : '';

    if (!input || !status) return;

    function setStatus(message, kind) {
      status.textContent = message;
      status.classList.remove('is-error', 'is-ok');
      if (kind) status.classList.add('is-' + kind);
    }

    input.addEventListener('input', function () {
      input.removeAttribute('aria-invalid');
      if (status.textContent) setStatus('', null);
    });

    form.addEventListener('submit', function (e) {
      e.preventDefault();
      var email = input.value.trim();

      if (!email) {
        input.setAttribute('aria-invalid', 'true');
        setStatus('Add an email address so we can send your invite.', 'error');
        input.focus();
        return;
      }

      if (!isValidEmail(email)) {
        input.setAttribute('aria-invalid', 'true');
        setStatus('That email looks incomplete — check for a typo.', 'error');
        input.focus();
        return;
      }

      saveEmail(email);
      form.classList.add('is-done');
      input.value = '';
      input.setAttribute('disabled', 'disabled');
      if (button) {
        button.innerHTML = 'On the list ✓';
        button.setAttribute('disabled', 'disabled');
      }
      setStatus("You're on the list. We'll email " + email + ' when an invite opens.', 'ok');

      // let the visitor add a second address if they want
      setTimeout(function () {
        form.classList.remove('is-done');
        input.removeAttribute('disabled');
        if (button) {
          button.innerHTML = originalLabel;
          button.removeAttribute('disabled');
        }
      }, 4200);
    });
  });

  /* ---------- footer year ---------- */
  var year = document.getElementById('year');
  if (year) year.textContent = String(new Date().getFullYear());
})();
