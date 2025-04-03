package com.springboot.MyTodoList.util;

public enum BotMessages {
	
	HELLO_MYTODO_BOT(
	"¡Bienvenido a MyTodoList Bot!\nEscribe una nueva tarea a continuación y presiona el botón de enviar (flecha azul), o selecciona una opción:"),
	BOT_REGISTERED_STARTED("Bot registrado e iniciado exitosamente!"),
	ITEM_DONE("¡Tarea completada! Selecciona /todolist para volver a la lista de tareas, o /start para ir a la pantalla principal."), 
	ITEM_UNDONE("Tarea marcada como pendiente. Selecciona /todolist para volver a la lista de tareas, o /start para ir a la pantalla principal."), 
	ITEM_DELETED("¡Tarea eliminada! Selecciona /todolist para volver a la lista de tareas, o /start para ir a la pantalla principal."),
	TYPE_NEW_TODO_ITEM("Escribe una nueva tarea a continuación y presiona el botón de enviar (flecha azul) en el lado derecho."),
	NEW_ITEM_ADDED("¡Nueva tarea añadida! Selecciona /todolist para volver a la lista de tareas, o /start para ir a la pantalla principal."),
	BYE("¡Hasta luego! Selecciona /start para continuar."),
	UNAUTHORIZED("Lo siento, no estás autorizado para usar este bot. Por favor, contacta con tu Manager para obtener acceso."),
	SHARE_PHONE("Por favor, comparte tu número de teléfono para verificar tu acceso.");

	private String message;

	BotMessages(String enumMessage) {
		this.message = enumMessage;
	}

	public String getMessage() {
		return message;
	}

}