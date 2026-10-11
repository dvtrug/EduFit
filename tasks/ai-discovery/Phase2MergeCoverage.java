import java.io.File;
import org.jacoco.core.tools.ExecFileLoader;

/** Merge real unit and app PostgreSQL execution data; no exclusions or synthetic probes. */
class Phase2MergeCoverage {
  public static void main(String[] args) throws Exception {
    if (args.length < 2) throw new IllegalArgumentException("destination, then execution-data inputs");
    var loader = new ExecFileLoader();
    for (int i = 1; i < args.length; i++) loader.load(new File(args[i]));
    loader.save(new File(args[0]), false);
    System.out.println("Merged " + (args.length - 1) + " JaCoCo execution-data files into " + args[0]);
  }
}
