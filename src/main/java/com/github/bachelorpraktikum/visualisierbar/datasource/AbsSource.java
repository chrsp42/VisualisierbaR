package com.github.bachelorpraktikum.visualisierbar.datasource;

import com.github.bachelorpraktikum.visualisierbar.logparser.GraphParser;
import com.github.bachelorpraktikum.visualisierbar.model.Context;
import com.github.bachelorpraktikum.visualisierbar.model.Element;
import com.github.bachelorpraktikum.visualisierbar.view.graph.Graph;

import javax.annotation.Nonnull;
import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

public class AbsSource implements DataSource {

    private static final Logger log = Logger.getLogger(DatabaseSource.class.getName());

    private final File fileToAbs;
    private final File fileToAbsSource;
    private final Context context;
    private final URI parent;
    private final String product;

    private ArrayList<String> delta;


    public AbsSource(String command, File path, String product) throws IOException {
        this.parent = new File(path.getParent()).toURI();
        this.fileToAbs = compileABS(command);
        this.fileToAbsSource = path;
        this.product = product;

        this.context = parseFile();
        this.delta = new ArrayList<>();
    }

    /**
     * Compiles a given ABS-File
     * @param command The complete shell-command to compile the ABS
     * @return the path to the compiled ABS-file
     * @throws IOException throws an Error, when the Input is not valid e.g. it's not an ABS-File
     */
    private File compileABS(String command) throws IOException {
        File file = new File(String.format("%s/actual.zug", this.parent.getPath()));

        ProcessBuilder builder = new ProcessBuilder();
        builder.directory(new File(this.parent.getPath()));
        builder.command("/bin/bash", "-c", String.format(
                "rm -r gen/erlang/*; %s; cd gen/erlang; ./run > %s/actual.zug;",
                command,
                this.parent.getPath()));

        Process process = builder.start();

        try {
            int exitCode = process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        return file;
    }

    private Context parseFile() throws IOException {
        return new GraphParser().parse(fileToAbs.getPath());
    }

    @Nonnull
    @Override
    public Context getContext() {
        return context;
    }

    /**
     * This method parses all used Deltas from the given product from the file.
     * It collect them in the ArrayList @var delta and return them.
     *
     * @return An ArrayList of all Deltas
     */

    public ArrayList<String> getDeltas(){
        ArrayList<Matcher> foundedDeltaLines = new ArrayList<>();
        ArrayList<String> productNames = new ArrayList<>();
        Matcher matcherProduct;
        Matcher matcherDelta;

        Pattern patternDeltaLines = compile("(delta )(.*?)(( after .*?)?)( when )(.*?)(;)");
        Pattern patternProduct = compile("(product "+this.product+" \\()(.*?)(\\);)");

        try {
            String aktLine;
            BufferedReader reader = new BufferedReader( new FileReader(this.fileToAbsSource+"/Run.abs") );
            while((aktLine = reader.readLine()) != null)
                {matcherDelta = patternDeltaLines.matcher(aktLine);
                 if(matcherDelta.find())
                    {foundedDeltaLines.add(matcherDelta);continue;}

                 matcherProduct = patternProduct.matcher(aktLine);
                 if(matcherProduct.find())
                    {productNames = new ArrayList<>(Arrays.asList(matcherProduct.group(2).split(", "))); break;}
                }
            reader.close();

            // Suchen der Deltas

            for (String productName : productNames) {
                for (Matcher foundedDeltaLine : foundedDeltaLines) {
                    if (foundedDeltaLine.group(6).matches("(|.*? )" + productName + "(| .*?)")) {
                        this.delta.add(foundedDeltaLine.group(2));
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return this.delta;
    }


    public void refactorSource(Graph graph)
        {File destDir = new File(fileToAbsSource.toString().concat("_new"));
         for(int i = 1; destDir.exists(); i++)
            {destDir = new File(fileToAbsSource.toString().concat("_new_" + i));}

         String newCode = "";

         if(copyFiles(destDir))
            {try {
                FileReader fr = new FileReader(destDir + "/Run.abs");
                BufferedReader br = new BufferedReader(fr);

                newCode = newCode.concat(searchForDelta(br, newCode));

                String zeile;
                while((zeile = br.readLine()) != null) {
                    newCode = newCode.concat(zeile).concat("\n");
                    if (zeile.contains("this.getApplication()")) {break;}
                }

                newCode = newCode.concat(graph.printNodesToAbs("\t\t"));
                newCode = newCode.concat("\n\n\n");
                newCode = newCode.concat(graph.printEdgesToAbs("\t\t"));
                newCode = newCode.concat("\n\n\n");


                while((zeile = br.readLine()) != null)
                    {if (zeile.contains("new TrainImpl") || zeile.contains("}")) {break;}
                     if (!(zeile.contains("new local") || zeile.contains(".add") || zeile.contains(".set") || zeile.replaceAll("(\t| )*", "").length() <= 2)) {
                        boolean found = false;
                        for (Element.Type t : Element.Type.values()) {
                            if (zeile.toLowerCase().contains(t.getName().toLowerCase())) found = true;
                        }
                        if (!found) {
                            newCode = newCode.concat(zeile).concat("\n");
                        }
                    }
                }

                newCode = newCode.concat("\n\n\n");
                newCode = newCode.concat(graph.printElementsToAbs("\t\t"));
                newCode = newCode.concat("\n\n\n");
                newCode = newCode.concat(graph.printLogicalGroupsToAbs("\t\t"));
                newCode = newCode.concat("\n\n\n");

                newCode = newCode.concat(zeile).concat("\n");
                while((zeile = br.readLine()) != null)
                    {newCode = newCode.concat(zeile).concat("\n");}

                FileWriter fw = new FileWriter(destDir+"/Run.abs");
                BufferedWriter bw = new BufferedWriter(fw);

                bw.write(newCode);

                bw.close();
                fw.close();
                fr.close();
                br.close();
            }
            catch(FileNotFoundException e){log.warning("Something went wrong while copying the ABS-Files");
            e.printStackTrace();}
            catch(IOException e){log.warning("Cannot read Run.abs");}
            }


        }

        private String searchForDelta(BufferedReader br, String newCode) throws IOException
            {String zeile;
             while((zeile = br.readLine()) != null)
                {newCode = newCode.concat(zeile).concat("\n");
                 for(String aDelta : delta) {
                    if (zeile.contains("delta " + aDelta + ";")) {
                        return newCode;
                       }
                   }
                }
             return newCode;
            }

    private boolean copyFiles(File destDir)
        {try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.directory(new File(this.parent.getPath()));
            builder.command("/bin/bash", "-c", String.format("cp -r %s %s", fileToAbsSource, destDir));

            Process process = builder.start();
            process.waitFor();
            return true;
         }
         catch(IOException e) {log.warning("Cannot copy ABS directory.");}
         catch(InterruptedException e){log.warning("Copying the ABS directory was interrupted.");}
         return false;
        }

    @Override
    public void close(){ }
}