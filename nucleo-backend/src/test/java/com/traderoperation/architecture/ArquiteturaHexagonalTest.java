package com.traderoperation.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.CompositeArchRule;
import java.util.List;

/** Trava as fronteiras hexagonais e entre módulos (D-11, RNF-06). Não desabilite: conserte o código. */
@AnalyzeClasses(packages = "com.traderoperation", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaHexagonalTest {

    /** Módulos de negócio. Ao criar um módulo novo, inclua-o aqui. */
    private static final List<String> MODULOS = List.of("autenticacao", "auditoria");

    @ArchTest
    static final ArchRule dominioNaoConheceFrameworks = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..", "org.hibernate..")
            .because("o domínio é Java puro");

    @ArchTest
    static final ArchRule aplicacaoNaoConheceInfraestrutura = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
            .because("a aplicação fala com o mundo só por ports");

    @ArchTest
    static final ArchRule sharedNaoConheceModulos = noClasses()
            .that().resideInAPackage("com.traderoperation.shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    MODULOS.stream().map(m -> "com.traderoperation." + m + "..").toArray(String[]::new))
            .because("shared é a base comum e não depende de nenhum módulo");

    @ArchTest
    static final ArchRule modulosSoConversamPorPortsDeEntrada = CompositeArchRule.of(
            MODULOS.stream().map(ArquiteturaHexagonalTest::acessoSoPorPortIn).toList());

    @ArchTest
    static final ArchRule semCiclosEntreModulos = slices()
            .matching("com.traderoperation.(*)..")
            .should().beFreeOfCycles();

    private static ArchRule acessoSoPorPortIn(String modulo) {
        String base = "com.traderoperation." + modulo;
        return noClasses()
                .that().resideOutsideOfPackage(base + "..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        base + ".domain..",
                        base + ".application.service..",
                        base + ".application.port.out..",
                        base + ".infrastructure..")
                .because("outros módulos só usam " + modulo + " pelo application.port.in");
    }
}
