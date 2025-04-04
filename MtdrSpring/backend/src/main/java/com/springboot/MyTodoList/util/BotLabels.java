//BotLabels.java
package com.springboot.MyTodoList.util;

public enum BotLabels {
	
	SHOW_MAIN_SCREEN("⬅️ Regresar a la pantalla principal"), 
	HIDE_MAIN_SCREEN("❌ Salir del Bot"),
	LIST_ALL_ITEMS("📋 Listado de Tareas"), 
	ADD_NEW_ITEM("➕ Añadir Nueva Tarea"),
	DONE("✅ DONE"),
	UNDO("🔄 UNDO"),
	DELETE("🗑️ DELETE"),
	MY_TODO_LIST("📝 MY TODO LIST"),
	DASH("➖");

	private String label;

	BotLabels(String enumLabel) {
		this.label = enumLabel;
	}

	public String getLabel() {
		return label;
	}

}
