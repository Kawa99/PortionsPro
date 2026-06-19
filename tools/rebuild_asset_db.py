import json
import os
import sqlite3
import time


DB_PATH = "app/src/main/assets/database/portions_pro.db"
SCHEMA_PATH = "app/schemas/com.example.mob_dev_portfolio.data.AppDatabase/3.json"


def read_identity_hash():
    with open(SCHEMA_PATH, encoding="utf-8") as schema_file:
        schema = json.load(schema_file)
    return schema["database"]["identityHash"]


def create_schema(cur, identity_hash):
    cur.executescript(
        """
        CREATE TABLE IF NOT EXISTS `recipes` (
            `id` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `category` TEXT NOT NULL,
            `area` TEXT NOT NULL,
            `instructions` TEXT NOT NULL,
            `thumbnail_url` TEXT NOT NULL,
            `source_url` TEXT NOT NULL,
            `base_servings` INTEGER NOT NULL,
            `prep_time_minutes` INTEGER NOT NULL,
            `cook_time_minutes` INTEGER NOT NULL,
            `is_user_created` INTEGER NOT NULL,
            `is_favourite` INTEGER NOT NULL,
            `is_cached` INTEGER NOT NULL,
            `created_at` INTEGER NOT NULL,
            `updated_at` INTEGER NOT NULL,
            PRIMARY KEY(`id`)
        );

        CREATE TABLE IF NOT EXISTS `ingredients` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `recipe_id` TEXT NOT NULL,
            `name` TEXT NOT NULL,
            `quantity` REAL NOT NULL,
            `unit` TEXT NOT NULL,
            `display_order` INTEGER NOT NULL,
            FOREIGN KEY(`recipe_id`) REFERENCES `recipes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
        );

        CREATE TABLE IF NOT EXISTS `shopping_list` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `ingredient_name` TEXT NOT NULL,
            `quantity` REAL NOT NULL,
            `unit` TEXT NOT NULL,
            `is_checked` INTEGER NOT NULL,
            `source_recipe_id` TEXT,
            `source_recipe_name` TEXT NOT NULL,
            `added_at` INTEGER NOT NULL
        );

        CREATE INDEX IF NOT EXISTS `index_ingredients_recipe_id` ON `ingredients` (`recipe_id`);

        CREATE VIEW `recipe_summary` AS SELECT
            r.id AS id,
            r.name AS name,
            r.category AS category,
            r.area AS area,
            r.thumbnail_url AS thumbnail_url,
            r.is_favourite AS is_favourite,
            r.is_user_created AS is_user_created,
            r.is_cached AS is_cached,
            r.base_servings AS base_servings,
            (r.prep_time_minutes + r.cook_time_minutes) AS total_time_minutes,
            (SELECT COUNT(*) FROM ingredients i WHERE i.recipe_id = r.id) AS ingredient_count
        FROM recipes r;

        CREATE TABLE IF NOT EXISTS room_master_table (
            id INTEGER PRIMARY KEY,
            identity_hash TEXT
        );
        """
    )
    cur.execute(
        "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES (42, ?)",
        (identity_hash,),
    )


def instructions_for(name, category, prep_minutes, cook_minutes):
    return "\n".join(
        [
            f"1. Prepare all ingredients for {name} and measure everything before cooking.",
            f"2. Spend about {prep_minutes} minutes chopping, rinsing and seasoning.",
            "3. Heat the main pan with oil or butter and start with the aromatics.",
            "4. Add the main ingredient and cook until it changes colour or begins to soften.",
            "5. Add the remaining ingredients and simmer, fry or bake until the texture is right.",
            f"6. Cook for about {cook_minutes} minutes in total, adjusting heat as needed.",
            f"7. Taste, season and serve the {category.lower()} while hot.",
        ]
    )


RECIPES = [
    ("r001", "Spaghetti Bolognese", "Pasta", "Italian", 4, 10, 40, [("Minced beef", 500, "g"), ("Spaghetti", 400, "g"), ("Onion", 1, ""), ("Carrot", 1, ""), ("Celery stalk", 1, ""), ("Chopped tomatoes", 400, "g"), ("Parmesan", 40, "g")]),
    ("r002", "Chicken and Rice", "Chicken", "British", 4, 10, 30, [("Chicken thighs", 600, "g"), ("Long-grain rice", 300, "g"), ("Chicken stock", 600, "ml"), ("Onion", 1, ""), ("Garlic cloves", 3, ""), ("Paprika", 1, "tsp"), ("Olive oil", 2, "tbsp")]),
    ("r003", "Red Lentil Soup", "Soup", "British", 4, 10, 30, [("Red lentils", 300, "g"), ("Onion", 1, ""), ("Carrots", 2, ""), ("Celery stalks", 2, ""), ("Vegetable stock", 1200, "ml"), ("Cumin", 1.5, "tsp"), ("Chopped tomatoes", 400, "g")]),
    ("r004", "Egg Fried Rice", "Rice", "Chinese", 4, 5, 15, [("Cold cooked rice", 600, "g"), ("Eggs", 3, ""), ("Frozen peas", 150, "g"), ("Spring onions", 3, ""), ("Soy sauce", 3, "tbsp"), ("Sesame oil", 1, "tsp"), ("Sunflower oil", 2, "tbsp")]),
    ("r005", "Bean Chilli", "Vegetarian", "Mexican", 4, 10, 25, [("Kidney beans", 400, "g"), ("Black beans", 400, "g"), ("Onion", 1, ""), ("Red peppers", 2, ""), ("Chopped tomatoes", 400, "g"), ("Cumin", 2, "tsp"), ("Smoked paprika", 1, "tsp")]),
    ("r006", "Dal Tadka", "Vegetarian", "Indian", 4, 10, 25, [("Red lentils", 300, "g"), ("Onion", 1, ""), ("Garlic cloves", 4, ""), ("Fresh ginger", 20, "g"), ("Tomatoes", 2, ""), ("Ghee", 2, "tbsp"), ("Garam masala", 1, "tsp")]),
    ("r007", "Chicken Curry", "Chicken", "Indian", 4, 15, 40, [("Chicken thighs", 700, "g"), ("Onions", 2, ""), ("Garlic cloves", 4, ""), ("Fresh ginger", 25, "g"), ("Chopped tomatoes", 400, "g"), ("Curry powder", 2, "tbsp"), ("Coriander", 20, "g")]),
    ("r008", "Aloo Gobi", "Vegetarian", "Indian", 4, 10, 25, [("Potatoes", 500, "g"), ("Cauliflower", 600, "g"), ("Onion", 1, ""), ("Garlic cloves", 2, ""), ("Turmeric", 0.5, "tsp"), ("Cumin", 1, "tsp"), ("Sunflower oil", 3, "tbsp")]),
    ("r009", "Shakshuka", "Eggs", "Tunisian", 4, 10, 25, [("Eggs", 8, ""), ("Chopped tomatoes", 800, "g"), ("Red peppers", 2, ""), ("Onion", 1, ""), ("Garlic cloves", 3, ""), ("Feta", 100, "g"), ("Cumin", 1, "tsp")]),
    ("r010", "Teriyaki Chicken Bowl", "Chicken", "Japanese", 4, 5, 20, [("Chicken thighs", 700, "g"), ("Jasmine rice", 300, "g"), ("Soy sauce", 4, "tbsp"), ("Mirin", 3, "tbsp"), ("Sugar", 1, "tbsp"), ("Spring onions", 3, ""), ("Sesame seeds", 1, "tbsp")]),
    ("r011", "Stir-Fried Noodles", "Noodles", "Chinese", 4, 10, 15, [("Dried noodles", 300, "g"), ("Chicken breast", 400, "g"), ("Garlic cloves", 3, ""), ("Spring onions", 4, ""), ("Soy sauce", 3, "tbsp"), ("Oyster sauce", 2, "tbsp"), ("Sesame oil", 1, "tsp")]),
    ("r012", "Korean Beef Rice Bowl", "Beef", "Korean", 4, 10, 15, [("Minced beef", 500, "g"), ("Jasmine rice", 300, "g"), ("Soy sauce", 4, "tbsp"), ("Sugar", 2, "tbsp"), ("Garlic cloves", 3, ""), ("Eggs", 4, ""), ("Spring onions", 3, "")]),
    ("r013", "Black Bean Tacos", "Vegetarian", "Mexican", 4, 10, 15, [("Black beans", 800, "g"), ("Corn tortillas", 8, ""), ("Onion", 1, ""), ("Garlic cloves", 2, ""), ("Cumin", 1.5, "tsp"), ("White cabbage", 150, "g"), ("Lime", 1, "")]),
    ("r014", "Greek Lemon Chicken", "Chicken", "Greek", 4, 35, 45, [("Chicken thighs", 700, "g"), ("Potatoes", 600, "g"), ("Lemons", 2, ""), ("Garlic cloves", 4, ""), ("Olive oil", 4, "tbsp"), ("Dried oregano", 2, "tsp"), ("Feta", 80, "g")]),
    ("r015", "Pasta e Fagioli", "Pasta", "Italian", 4, 10, 25, [("Cannellini beans", 800, "g"), ("Small pasta", 200, "g"), ("Onion", 1, ""), ("Garlic cloves", 3, ""), ("Vegetable stock", 800, "ml"), ("Rosemary", 1, "sprig"), ("Parmesan", 40, "g")]),
    ("r016", "Chickpea Stew", "Vegetarian", "Spanish", 4, 10, 25, [("Chickpeas", 800, "g"), ("Spinach", 200, "g"), ("Onion", 1, ""), ("Red peppers", 2, ""), ("Chopped tomatoes", 400, "g"), ("Vegetable stock", 300, "ml"), ("Smoked paprika", 2, "tsp")]),
    ("r017", "Jollof Rice", "Rice", "Nigerian", 4, 15, 45, [("Long-grain rice", 400, "g"), ("Tomatoes", 400, "g"), ("Red peppers", 2, ""), ("Onions", 2, ""), ("Tomato paste", 3, "tbsp"), ("Chicken stock", 700, "ml"), ("Dried thyme", 1, "tsp")]),
    ("r018", "Turkish Red Lentil Soup", "Soup", "Turkish", 4, 10, 25, [("Red lentils", 300, "g"), ("Onion", 1, ""), ("Garlic cloves", 3, ""), ("Vegetable stock", 1200, "ml"), ("Butter", 30, "g"), ("Cumin", 1.5, "tsp"), ("Lemon", 1, "")]),
    ("r019", "Butter Bean Tagine", "Vegetarian", "Moroccan", 4, 10, 30, [("Butter beans", 800, "g"), ("Spinach", 200, "g"), ("Onion", 1, ""), ("Garlic cloves", 3, ""), ("Chopped tomatoes", 400, "g"), ("Harissa paste", 2, "tbsp"), ("Cinnamon", 0.5, "tsp")]),
    ("r020", "Nasi Goreng", "Rice", "Indonesian", 4, 5, 15, [("Cold cooked rice", 600, "g"), ("Chicken thighs", 400, "g"), ("Eggs", 4, ""), ("Shallots", 3, ""), ("Garlic cloves", 3, ""), ("Kecap manis", 2, "tbsp"), ("Soy sauce", 2, "tbsp")]),
    ("r021", "Chicken Satay", "Chicken", "Malaysian", 4, 35, 10, [("Chicken breast", 600, "g"), ("Peanut butter", 6, "tbsp"), ("Coconut milk", 200, "ml"), ("Soy sauce", 2, "tbsp"), ("Lime", 1, ""), ("Garlic cloves", 2, ""), ("Red chilli", 1, "")]),
    ("r022", "Thai Basil Chicken", "Chicken", "Thai", 4, 5, 15, [("Minced chicken", 600, "g"), ("Thai basil", 40, "g"), ("Garlic cloves", 4, ""), ("Red chillies", 2, ""), ("Fish sauce", 2, "tbsp"), ("Oyster sauce", 1, "tbsp"), ("Jasmine rice", 300, "g")]),
    ("r023", "Tofu Stir Fry", "Vegetarian", "Chinese", 4, 25, 15, [("Firm tofu", 400, "g"), ("Broccoli", 300, "g"), ("Red pepper", 1, ""), ("Garlic cloves", 3, ""), ("Fresh ginger", 20, "g"), ("Soy sauce", 3, "tbsp"), ("Sesame oil", 1, "tsp")]),
    ("r024", "Chilli Con Carne", "Beef", "American", 4, 10, 35, [("Minced beef", 500, "g"), ("Kidney beans", 400, "g"), ("Onion", 1, ""), ("Garlic cloves", 3, ""), ("Chopped tomatoes", 400, "g"), ("Chilli powder", 1, "tsp"), ("Cumin", 2, "tsp")]),
    ("r025", "Mac and Cheese", "Pasta", "American", 4, 10, 20, [("Macaroni", 400, "g"), ("Cheddar", 250, "g"), ("Whole milk", 500, "ml"), ("Butter", 40, "g"), ("Plain flour", 40, "g"), ("Dijon mustard", 1, "tsp"), ("Breadcrumbs", 60, "g")]),
    ("r026", "Tuna Pasta", "Pasta", "British", 4, 5, 15, [("Pasta", 400, "g"), ("Tuna", 160, "g"), ("Chopped tomatoes", 400, "g"), ("Garlic cloves", 2, ""), ("Olive oil", 2, "tbsp"), ("Black olives", 80, "g"), ("Parsley", 15, "g")]),
    ("r027", "Fish Pie", "Seafood", "British", 4, 20, 45, [("White fish", 600, "g"), ("Potatoes", 800, "g"), ("Whole milk", 500, "ml"), ("Butter", 60, "g"), ("Plain flour", 40, "g"), ("Frozen peas", 150, "g"), ("Eggs", 2, "")]),
    ("r028", "Chicken Thigh Traybake", "Chicken", "British", 4, 15, 45, [("Chicken thighs", 700, "g"), ("Potatoes", 600, "g"), ("Red peppers", 2, ""), ("Red onion", 1, ""), ("Olive oil", 3, "tbsp"), ("Smoked paprika", 2, "tsp"), ("Garlic cloves", 4, "")]),
    ("r029", "Egg and Potato Hash", "Eggs", "British", 4, 10, 20, [("Potatoes", 600, "g"), ("Eggs", 8, ""), ("Onion", 1, ""), ("Sunflower oil", 3, "tbsp"), ("Smoked paprika", 1, "tsp"), ("Parsley", 10, "g"), ("Cheddar", 60, "g")]),
    ("r030", "Shakshuka with Feta", "Eggs", "Israeli", 4, 10, 30, [("Eggs", 8, ""), ("Chopped tomatoes", 800, "g"), ("Yellow pepper", 1, ""), ("Onion", 1, ""), ("Garlic cloves", 3, ""), ("Feta", 100, "g"), ("Pitta breads", 4, "")]),
    ("r031", "Saag Aloo", "Vegetarian", "Indian", 4, 10, 20, [("Potatoes", 500, "g"), ("Spinach", 400, "g"), ("Garlic cloves", 3, ""), ("Fresh ginger", 20, "g"), ("Cumin seeds", 1, "tsp"), ("Turmeric", 0.5, "tsp"), ("Lemon", 0.5, "")]),
    ("r032", "Lemon Herb Chicken Couscous", "Chicken", "Moroccan", 4, 10, 20, [("Chicken thighs", 700, "g"), ("Couscous", 300, "g"), ("Chicken stock", 400, "ml"), ("Lemons", 2, ""), ("Olive oil", 3, "tbsp"), ("Cumin", 1, "tsp"), ("Parsley", 20, "g")]),
    ("r033", "Chickpea and Spinach Curry", "Vegetarian", "Indian", 4, 10, 20, [("Chickpeas", 800, "g"), ("Spinach", 200, "g"), ("Onion", 1, ""), ("Garlic cloves", 3, ""), ("Chopped tomatoes", 400, "g"), ("Garam masala", 1, "tsp"), ("Basmati rice", 300, "g")]),
    ("r034", "Lentil and Egg Curry", "Eggs", "Indian", 4, 10, 30, [("Red lentils", 300, "g"), ("Eggs", 8, ""), ("Onion", 1, ""), ("Garlic cloves", 3, ""), ("Chopped tomatoes", 400, "g"), ("Vegetable stock", 600, "ml"), ("Turmeric", 0.5, "tsp")]),
    ("r035", "Ful Medames", "Beans", "Egyptian", 4, 5, 15, [("Fava beans", 800, "g"), ("Lemons", 2, ""), ("Garlic cloves", 3, ""), ("Olive oil", 3, "tbsp"), ("Cumin", 1.5, "tsp"), ("Parsley", 20, "g"), ("Pitta breads", 4, "")]),
    ("r036", "Borscht", "Soup", "Ukrainian", 4, 15, 40, [("Beetroot", 400, "g"), ("Carrots", 2, ""), ("Potatoes", 2, ""), ("Onion", 1, ""), ("White cabbage", 300, "g"), ("Vegetable stock", 1200, "ml"), ("Kidney beans", 400, "g")]),
    ("r037", "Cabbage and Potato Soup", "Soup", "Polish", 4, 10, 25, [("Potatoes", 600, "g"), ("White cabbage", 400, "g"), ("Onion", 1, ""), ("Vegetable stock", 1200, "ml"), ("Smoked paprika", 1.5, "tsp"), ("Sunflower oil", 2, "tbsp"), ("Rye bread", 4, "slices")]),
    ("r038", "Huevos Rancheros", "Eggs", "Mexican", 4, 5, 20, [("Eggs", 8, ""), ("Corn tortillas", 8, ""), ("Chopped tomatoes", 400, "g"), ("Onion", 1, ""), ("Garlic cloves", 2, ""), ("Green chilli", 1, ""), ("Cumin", 1, "tsp")]),
    ("r039", "Rice and Beans", "Vegetarian", "Mexican", 4, 5, 25, [("Long-grain rice", 300, "g"), ("Kidney beans", 400, "g"), ("Onion", 1, ""), ("Garlic cloves", 2, ""), ("Vegetable stock", 600, "ml"), ("Tomato puree", 1, "tbsp"), ("Coriander", 20, "g")]),
    ("r040", "Mie Goreng", "Noodles", "Indonesian", 4, 5, 15, [("Egg noodles", 300, "g"), ("Eggs", 3, ""), ("Garlic cloves", 3, ""), ("Green chilli", 1, ""), ("Soy sauce", 2, "tbsp"), ("Oyster sauce", 2, "tbsp"), ("Kecap manis", 1, "tbsp")]),
    ("r041", "Chicken Fajitas", "Chicken", "Mexican", 4, 10, 20, [("Chicken breast", 600, "g"), ("Flour tortillas", 8, ""), ("Red peppers", 2, ""), ("Yellow pepper", 1, ""), ("Onion", 1, ""), ("Cumin", 1.5, "tsp"), ("Sour cream", 120, "g")]),
    ("r042", "Congee", "Rice", "Chinese", 4, 5, 70, [("Jasmine rice", 200, "g"), ("Chicken stock", 1600, "ml"), ("Spring onions", 3, ""), ("Soy sauce", 2, "tbsp"), ("Sesame oil", 1, "tsp"), ("Eggs", 4, ""), ("White pepper", 0.5, "tsp")]),
    ("r043", "Chicken Quesadillas", "Chicken", "Mexican", 4, 10, 15, [("Chicken thighs", 500, "g"), ("Flour tortillas", 8, ""), ("Cheddar", 200, "g"), ("Cumin", 1, "tsp"), ("Smoked paprika", 1, "tsp"), ("Garlic cloves", 2, ""), ("Salsa", 150, "g")]),
    ("r044", "Masoor Dal", "Beans", "Pakistani", 4, 5, 25, [("Red lentils", 300, "g"), ("Onion", 1, ""), ("Garlic cloves", 4, ""), ("Fresh ginger", 20, "g"), ("Tomatoes", 2, ""), ("Cumin seeds", 1, "tsp"), ("Garam masala", 0.5, "tsp")]),
    ("r045", "Garlic Prawn Pasta", "Seafood", "Italian", 2, 5, 15, [("Raw prawns", 400, "g"), ("Linguine", 300, "g"), ("Garlic cloves", 4, ""), ("Olive oil", 4, "tbsp"), ("Chilli flakes", 0.5, "tsp"), ("Lemon", 1, ""), ("Parsley", 15, "g")]),
    ("r046", "Buffalo Chicken Wraps", "Chicken", "American", 4, 15, 20, [("Chicken breast", 600, "g"), ("Flour tortillas", 8, ""), ("Breadcrumbs", 100, "g"), ("Eggs", 2, ""), ("Hot sauce", 4, "tbsp"), ("Butter", 30, "g"), ("Lettuce", 100, "g")]),
    ("r047", "Tofu Scramble", "Vegetarian", "American", 4, 5, 15, [("Firm tofu", 400, "g"), ("Turmeric", 0.5, "tsp"), ("Garlic powder", 0.5, "tsp"), ("Cumin", 0.5, "tsp"), ("Nutritional yeast", 2, "tbsp"), ("Tomatoes", 2, ""), ("Spinach", 100, "g")]),
    ("r048", "Sardine Toast", "Seafood", "Portuguese", 4, 5, 5, [("Canned sardines", 240, "g"), ("Sourdough bread", 4, "slices"), ("Lemon", 1, ""), ("Parsley", 15, "g"), ("Olive oil", 1, "tbsp"), ("Tomatoes", 2, ""), ("Black pepper", 0.5, "tsp")]),
    ("r049", "Chicken Soup", "Chicken", "British", 4, 15, 75, [("Chicken pieces", 1000, "g"), ("Onion", 1, ""), ("Carrots", 2, ""), ("Celery stalks", 2, ""), ("Bay leaves", 2, ""), ("Egg noodles", 150, "g"), ("Leek", 1, "")]),
    ("r050", "Vegetable Fried Rice", "Rice", "Chinese", 4, 5, 15, [("Cold cooked rice", 600, "g"), ("Eggs", 3, ""), ("Frozen peas", 150, "g"), ("Carrot", 1, ""), ("Spring onions", 3, ""), ("Soy sauce", 3, "tbsp"), ("Sesame oil", 1, "tsp")]),
]

EXTRA_INGREDIENTS = {
    "r001": [("Black pepper", 1, "tsp")],
    "r002": [("Fresh parsley", 15, "g")],
    "r003": [("Lemon juice", 1, "tbsp")],
    "r004": [("White pepper", 0.5, "tsp")],
    "r005": [("Cheddar", 80, "g")],
    "r006": [("Coriander", 20, "g")],
    "r007": [("Basmati rice", 300, "g")],
    "r008": [("Coriander", 20, "g")],
    "r009": [("Parsley", 15, "g")],
    "r010": [("Broccoli", 300, "g")],
    "r011": [("Carrot", 1, "")],
    "r012": [("Sesame seeds", 1, "tbsp")],
    "r013": [("Tomatoes", 2, "")],
    "r014": [("Greek yoghurt", 120, "g")],
    "r015": [("Black pepper", 1, "tsp")],
    "r016": [("Crusty bread", 4, "slices")],
    "r017": [("Bay leaves", 2, "")],
    "r018": [("Chilli flakes", 0.5, "tsp")],
    "r019": [("Flatbreads", 4, "")],
    "r020": [("Cucumber", 1, "")],
}


def rebuild():
    identity_hash = read_identity_hash()
    os.makedirs(os.path.dirname(DB_PATH), exist_ok=True)
    if os.path.exists(DB_PATH):
        os.remove(DB_PATH)

    conn = sqlite3.connect(DB_PATH)
    try:
        cur = conn.cursor()
        cur.execute("PRAGMA foreign_keys = ON")
        create_schema(cur, identity_hash)

        now_ms = int(time.time() * 1000)
        recipe_rows = []
        ingredient_rows = []
        for recipe_id, name, category, area, servings, prep, cook, ingredients in RECIPES:
            recipe_rows.append(
                (
                    recipe_id,
                    name,
                    category,
                    area,
                    instructions_for(name, category, prep, cook),
                    "",
                    "",
                    servings,
                    prep,
                    cook,
                    0,
                    0,
                    1,
                    now_ms,
                    now_ms,
                )
            )
            for display_order, (ingredient_name, quantity, unit) in enumerate(ingredients):
                ingredient_rows.append((recipe_id, ingredient_name, float(quantity), unit, display_order))
            for offset, (ingredient_name, quantity, unit) in enumerate(EXTRA_INGREDIENTS.get(recipe_id, [])):
                ingredient_rows.append((recipe_id, ingredient_name, float(quantity), unit, len(ingredients) + offset))

        cur.executemany(
            """
            INSERT INTO recipes (
                id, name, category, area, instructions, thumbnail_url, source_url,
                base_servings, prep_time_minutes, cook_time_minutes,
                is_user_created, is_favourite, is_cached, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            recipe_rows,
        )
        cur.executemany(
            """
            INSERT INTO ingredients (recipe_id, name, quantity, unit, display_order)
            VALUES (?, ?, ?, ?, ?)
            """,
            ingredient_rows,
        )
        cur.execute("PRAGMA user_version = 3")
        conn.commit()

        recipe_count = cur.execute("SELECT COUNT(*) FROM recipes").fetchone()[0]
        ingredient_count = cur.execute("SELECT COUNT(*) FROM ingredients").fetchone()[0]
        user_version = cur.execute("PRAGMA user_version").fetchone()[0]
        hash_in_db = cur.execute("SELECT identity_hash FROM room_master_table WHERE id = 42").fetchone()[0]
        zero_time_count = cur.execute(
            "SELECT COUNT(*) FROM recipes WHERE prep_time_minutes = 0 AND cook_time_minutes = 0"
        ).fetchone()[0]
        summary_count = cur.execute("SELECT COUNT(*) FROM recipe_summary").fetchone()[0]

        assert recipe_count == 50, recipe_count
        assert ingredient_count == 370, ingredient_count
        assert user_version == 3, user_version
        assert hash_in_db == identity_hash, hash_in_db
        assert zero_time_count == 0, zero_time_count
        assert summary_count == 50, summary_count

        print("Done.")
        print(f"Recipes:     {recipe_count}")
        print(f"Ingredients: {ingredient_count}")
        print(f"user_version: {user_version}")
        print(f"identity_hash: {hash_in_db}")
        print(f"Hash matches schema JSON: {hash_in_db == identity_hash}")
        print(f"Recipes with zero total time: {zero_time_count}")
    finally:
        conn.close()


if __name__ == "__main__":
    rebuild()
