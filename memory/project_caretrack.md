---
name: CareTrack — Contexto del proyecto
description: Propósito, etapa y roadmap de CareTrack
type: project
---

CareTrack es un asistente de salud/cuidado familiar con interfaz de chat. El backend es un chatbot con lógica de reglas propia (no LLM externo). El proyecto está en fase MVP/prototipo.

**Why:** Ayudar a familias a gestionar el cuidado de sus miembros (pacientes), con un chatbot que guía y asiste en temas de salud.

**Próximas funcionalidades prioritarias:**
- Autenticación y gestión de usuarios
- Gestión de perfil
- Pacientes asociados a una familia concreta
- Administración de familia: información, miembros y roles dentro de la familia

**How to apply:** Al sugerir nuevas features o arquitectura, tener en cuenta que el modelo de datos girará en torno a Familia → Miembros (con roles) → Pacientes. La autenticación es el siguiente paso crítico antes de construir estas capas.
