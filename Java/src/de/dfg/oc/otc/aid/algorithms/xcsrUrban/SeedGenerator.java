package de.dfg.oc.otc.aid.algorithms.xcsrUrban;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by dmrauh on 02.03.15.
 *
 * @author rauhdomi
 *         <p>
 *         Holds seeds for the random generators. (Momentarily statically 30 true random Seeds)
 *         Or alternatively an infinite amount of pesudo random seeds.
 */
final class SeedGenerator {
    private static final int TRUE_RANDOM_SEEDS_COUNT;
    private static final Set<Long> TRUE_RANDOM_SEEDS = new LinkedHashSet<>(
            Arrays.asList(
                    830473998L,
                    403834299L,
                    238965585L,
                    408142638L,
                    942717059L,
                    281379800L,
                    368686664L,
                    810244816L,
                    294075679L,
                    773666111L,
                    589692214L,
                    185784654L,
                    316694866L,
                    934074476L,
                    848114577L,
                    600564467L,
                    546526942L,
                    647767920L,
                    340089806L,
                    532650536L,
                    960592243L,
                    189889986L,
                    985561741L,
                    714632367L,
                    999256368L,
                    528695203L,
                    240380654L,
                    591742309L,
                    808896301L,
                    204196615L,
                    119804382L,
                    916960036L,
                    432840764L,
                    860147564L,
                    803358888L,
                    759182069L,
                    603265218L,
                    186222096L,
                    899370977L,
                    862031675L,
                    495151803L,
                    370500467L,
                    333321847L,
                    888775518L,
                    682051221L,
                    754591240L,
                    502327920L,
                    876763656L,
                    994207594L,
                    426657422L,
                    545858183L,
                    361942080L,
                    257738496L,
                    764280686L,
                    676850663L,
                    796168178L,
                    571675405L,
                    582999701L,
                    754701950L,
                    469575552L,
                    203917255L,
                    127362557L,
                    870077783L,
                    936016259L,
                    550675276L,
                    891770844L,
                    284649611L,
                    285272924L,
                    267895063L,
                    961505799L,
                    773818448L,
                    101689782L,
                    674478921L,
                    723299785L,
                    538633246L,
                    234569651L,
                    583762700L,
                    258850586L,
                    822650122L,
                    323511661L,
                    292813405L,
                    294305618L,
                    660797267L,
                    274642556L,
                    409050269L,
                    290116699L,
                    217227305L,
                    760805725L,
                    456863039L,
                    230061554L,
                    290371511L,
                    899671019L,
                    508986611L,
                    411289783L,
                    916274567L,
                    489990128L,
                    130494573L,
                    117119148L,
                    682102043L,
                    919493966L,
                    524738154L,
                    722622651L,
                    347029217L,
                    413673437L,
                    814792980L,
                    150210816L,
                    701616888L,
                    364607901L,
                    508604684L,
                    381974136L,
                    765896942L,
                    431768123L,
                    493717978L,
                    817845095L,
                    120276342L,
                    924910425L,
                    682478516L,
                    470280143L,
                    104968653L,
                    312225776L,
                    435806219L,
                    378844068L,
                    562270336L,
                    772499282L,
                    483182832L,
                    274979946L,
                    516619205L,
                    824206647L,
                    555950150L,
                    888653642L
            )
    );

    static {
        TRUE_RANDOM_SEEDS_COUNT = TRUE_RANDOM_SEEDS.size();
    }

    private SeedGenerator() {
    }

    /**
     * Returns a truly random seed for a given experiment number (30 max)
     *
     * @param numberOfExperiment The number of the experiment to get the true random seed for (starting at 1)
     * @return The true random seed
     */
    static long getTrueRandomSeedForExperiment(int numberOfExperiment) {
        if (numberOfExperiment < 1) {
            throw new IllegalArgumentException("Number of Experiment has to start at 1!");
        } else if (numberOfExperiment > TRUE_RANDOM_SEEDS_COUNT) {
            throw new IllegalArgumentException("Number of Experiments too high. Generator only has "
                    + TRUE_RANDOM_SEEDS.size()
                    + " true random seeds!");
        }
        return TRUE_RANDOM_SEEDS.toArray(new Long[TRUE_RANDOM_SEEDS_COUNT])[numberOfExperiment - 1];
    }
}
