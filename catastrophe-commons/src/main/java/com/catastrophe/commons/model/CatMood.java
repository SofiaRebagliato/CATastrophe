package com.catastrophe.commons.model;

/**
 * Modelo exhaustivo de estados de ánimo felinos.
 * 
 * Al ser sealed, el compilador garantiza que todo switch los cubra todos.
 * Esto es una de las features clave de Java 21 que queremos practicar.
 * 
 * Ejemplo de uso con pattern matching:
 * <pre>{@code
 *     String emoji = switch (mood) {
 *         case CatMood.Curious c   -> "🔍";
 *         case CatMood.Playful p   -> "🎾";
 *         case CatMood.Sleepy s    -> "😴";
 *         case CatMood.Hungry h    -> "🍖";
 *         case CatMood.Grumpy g    -> "😾";
 *         case CatMood.Affectionate a -> "😻";
 *         case CatMood.Mysterious m   -> "🌙";
 *     };
 * }</pre>
 */
public sealed interface CatMood {

    String displayName();
    String description();

    record Curious() implements CatMood {
        @Override public String displayName() { return "Curioso"; }
        @Override public String description() { return "Explorando cada rincón con ojos bien abiertos"; }
    }

    record Playful() implements CatMood {
        @Override public String displayName() { return "Juguetón"; }
        @Override public String description() { return "Persiguiendo todo lo que se mueve"; }
    }

    record Sleepy() implements CatMood {
        @Override public String displayName() { return "Dormilón"; }
        @Override public String description() { return "Buscando el rayo de sol perfecto para una siesta"; }
    }

    record Hungry() implements CatMood {
        @Override public String displayName() { return "Hambriento"; }
        @Override public String description() { return "Maullando junto al plato vacío"; }
    }

    record Grumpy() implements CatMood {
        @Override public String displayName() { return "Gruñón"; }
        @Override public String description() { return "No me toques. No me mires. No respires."; }
    }

    record Affectionate() implements CatMood {
        @Override public String displayName() { return "Cariñoso"; }
        @Override public String description() { return "Ronroneando y buscando caricias"; }
    }

    record Mysterious() implements CatMood {
        @Override public String displayName() { return "Misterioso"; }
        @Override public String description() { return "Mirando fijamente una esquina vacía..."; }
    }
}
