package com.lowdragmc.mbd2.test.tests.editor;

import com.lowdragmc.mbd2.MBD2;
import com.lowdragmc.mbd2.api.recipe.MBDRecipeType;
import com.lowdragmc.mbd2.test.tests.MBDSmokeFixtures;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * The recipe type search box of the machine editor (and of the fuel recipe list). Since LDLib2
 * 2.2.40 it searches the whole catalogue as soon as it opens, which put a registered type that is
 * also a project on disk in the list twice, and every project on disk in it whatever was typed.
 *
 * <p>The projects are written to a temp folder, never to the {@code recipe_type} folder the game
 * loads at launch: a leftover project naming a fixture's id would abort the next launch.
 */
@GameTestHolder(MBD2.MOD_ID)
public class RecipeTypeSearchTests {
    static { @SuppressWarnings("unused") var ignored = MBDSmokeFixtures.RECIPE_TYPE_ID; }

    private static final ResourceLocation UNREGISTERED = MBD2.id("search_test_unregistered_project");

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_type_search_offers_each_id_once(GameTestHelper helper) {
        var registered = MBDSmokeFixtures.RECIPE_TYPE_ID;
        withProjects(helper, folder -> {
            writeProject(folder.resolve("registered.rt"), registered);
            writeProject(folder.resolve("nested/unregistered.rt"), UNREGISTERED);
            writeProject(folder.resolve("nested/unregistered_copy.rt"), UNREGISTERED);

            var all = search(folder, "", null);
            helper.assertTrue(all.size() == new HashSet<>(all).size(), "An id was offered more than once: " + all);
            helper.assertTrue(all.contains(MBDRecipeType.DUMMY.getRegistryName()), "Dummy missing: " + all);
            helper.assertTrue(all.contains(registered), "Registered type missing: " + all);
            helper.assertTrue(all.contains(UNREGISTERED), "Project not registered yet missing: " + all);
        });
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_type_search_filters_projects_by_word(GameTestHelper helper) {
        var registered = MBDSmokeFixtures.RECIPE_TYPE_ID;
        withProjects(helper, folder -> {
            writeProject(folder.resolve("registered.rt"), registered);
            writeProject(folder.resolve("unregistered.rt"), UNREGISTERED);

            var smoke = search(folder, "SMOKE_recipe", null);
            helper.assertTrue(smoke.contains(registered), "Matching type missing: " + smoke);
            helper.assertTrue(!smoke.contains(UNREGISTERED), "A project that does not match was offered: " + smoke);

            var unregistered = search(folder, "search_test_unregistered", null);
            helper.assertTrue(unregistered.equals(List.of(UNREGISTERED)), "Expected only the matching project: " + unregistered);
        });
    }

    @GameTest(template = "empty_simple")
    @PrefixGameTestTemplate(false)
    public static void recipe_type_search_leaves_out_the_excluded_id(GameTestHelper helper) {
        withProjects(helper, folder -> {
            writeProject(folder.resolve("unregistered.rt"), UNREGISTERED);

            var result = search(folder, "", UNREGISTERED);
            helper.assertTrue(!result.contains(UNREGISTERED), "The excluded id was offered from disk: " + result);
            var dummy = search(folder, "", MBDRecipeType.DUMMY.getRegistryName());
            helper.assertTrue(!dummy.contains(MBDRecipeType.DUMMY.getRegistryName()), "The excluded dummy was offered: " + dummy);
        });
    }

    private interface ProjectsBody {
        void run(Path folder) throws IOException;
    }

    private static void withProjects(GameTestHelper helper, ProjectsBody body) {
        Path folder = null;
        try {
            folder = Files.createTempDirectory("mbd2-recipe-type-search");
            body.run(folder);
        } catch (IOException e) {
            helper.fail("Could not write the test projects: " + e);
            return;
        } finally {
            if (folder != null) {
                deleteRecursively(folder);
            }
        }
        helper.succeed();
    }

    private static List<ResourceLocation> search(Path folder, String word, @Nullable ResourceLocation exclude) {
        var result = new ArrayList<ResourceLocation>();
        MBDRecipeType.searchRecipeTypeIds(folder.toFile(), word, exclude, result::add);
        return result;
    }

    /** The layout {@code RecipeTypeProject} saves: {@code data.recipe_type.registryName}. */
    private static void writeProject(Path file, ResourceLocation registryName) throws IOException {
        Files.createDirectories(file.getParent());
        var recipeType = new CompoundTag();
        recipeType.putString("registryName", registryName.toString());
        var data = new CompoundTag();
        data.put("recipe_type", recipeType);
        var root = new CompoundTag();
        root.put("data", data);
        NbtIo.write(root, file);
    }

    private static void deleteRecursively(Path folder) {
        try (var paths = Files.walk(folder)) {
            paths.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        } catch (IOException ignored) {
        }
    }
}
