/*
 * Created on 09-Feb-2006
 * Copyright (C) 2006 by Andrea Vacondio.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the 
 * GNU General Public License as published by the Free Software Foundation; 
 * either version 2 of the License.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program; 
 * if not, write to the Free Software Foundation, Inc., 
 *  59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package it.pdfsam.console;

import it.pdfsam.console.events.WorkDoneEvent;
import it.pdfsam.console.interfaces.WorkDoneListener;
import it.pdfsam.console.tools.CmdParser;
import it.pdfsam.console.tools.PdfConcat;
import it.pdfsam.console.tools.PdfEncrypt;
import it.pdfsam.console.tools.PdfSplit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.event.EventListenerList;

import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import com.lowagie.text.Document;

/**
 * Console class.
 * It takes input arguments, parse tham and execute the right operation on pdf files.
 * 
 * @author Andrea Vacondio
 * @see it.pdfsam.console.tools.CmdParser
 * @see it.pdfsam.console.tools.PdfConcat
 * @see it.pdfsam.console.tools.PdfSplit
 * @see it.pdfsam.console.exception.ParseException
 * @see it.pdfsam.console.exception.SplitException
 */
public class MainConsole{

    //list of listeners
    private EventListenerList listeners = new EventListenerList();
    /**
     * Console version
     */
    public static final String VERSION = "0.6.2e"; 
    public static final String CREATOR = "pdfsam-console (Ver. " +MainConsole.VERSION+ ")";
       
    public static void main(String[] args){
        try{            
            MainConsole mc = new MainConsole();            
            System.out.println(mc.mainAction(args, false));
        }catch(Exception ex){
            System.out.println(ex.getMessage());
        }
    }

    /**
     * It takes input parameters, parse tham. If no exception is thrown it executes the right function (split, concat..)
     * and return the output message.
     * 
     * @param args Arguments String array.
     * @param html_output  If the output message is HTML or plain text.
     * @return Output message.
     * @throws Exception If something goes wrong an exception is thrown.
     */
    public synchronized String mainAction(String[] args, boolean html_output) throws Exception{
        String out_msg = "";
        //command parser creation
        CmdParser cmdp = new CmdParser(args);
        //parsing
        cmdp.Parse();
        //if it's a concat
        if ((cmdp.getInputCommand()) == CmdParser.C_CONCAT){
            //and it a -f option
            if (cmdp.getInputOption() == CmdParser.F_OPT){
                PdfConcat pdf_concatenator = new PdfConcat(cmdp.getCFValue(), cmdp.getOValue(), cmdp.getCUValue(), cmdp.COverwrite(), this);
                pdf_concatenator.doConcat();
                if (html_output) {
                    out_msg = pdf_concatenator.getOutHTMLMessage();
                }
                else{
                    out_msg = pdf_concatenator.getOutMessage();
                }
            }
            else if(cmdp.getInputOption() == CmdParser.L_OPT){
                File l_file = cmdp.getCLValue();
                Collection file_list = null;
				if (getExtension(l_file).equals("XML".toLowerCase())){
					file_list = parseXmlFile(l_file);
				}
				if (getExtension(l_file).equals("CVS".toLowerCase())){
					file_list = parseCsvFile(l_file);
				}                
                if (file_list == null){
                    out_msg = "Error reading csv or xml file-";
                }else{
                    PdfConcat pdf_concatenator = new PdfConcat(file_list, cmdp.getOValue(), cmdp.getCUValue(), cmdp.COverwrite(), this);
                    pdf_concatenator.doConcat();
                    if (html_output) {
                        out_msg = pdf_concatenator.getOutHTMLMessage();
                    }
                    else{
                        out_msg = pdf_concatenator.getOutMessage();
                    }
                }
            }
        }
        else if ((cmdp.getInputCommand()) == CmdParser.C_SPLIT){
            PdfSplit pdf_splitter = new PdfSplit(cmdp.getOValue(), cmdp.getSFValue(), cmdp.getSPValue(), cmdp.getSSValue(), cmdp.getSNumberPageValue(), this);
            pdf_splitter.doSplit();
            if (html_output) {
                out_msg = pdf_splitter.getOutHTMLMessage();
            }
            else{
                out_msg = pdf_splitter.getOutMessage();
            }
        }
        else if ((cmdp.getInputCommand()) == CmdParser.C_ECRYPT){
        	PdfEncrypt pdf_encrypt = new PdfEncrypt(cmdp.getOValue(), cmdp.getEFValue(), cmdp.getEAllowValue(), cmdp.getEUpwdValue(), cmdp.getEApwdValue(), cmdp.getEPValue(), cmdp.getETypeValue(), this);
        	pdf_encrypt.doEncrypt();
            if (html_output) {
                out_msg = pdf_encrypt.getOutHTMLMessage();
            }
            else{
                out_msg = pdf_encrypt.getOutMessage();
            }       	
        }
        	
        //try to write on output file
        try{
            File log_out_file =cmdp.getLogValue(); 
            if (log_out_file != null){
                /*writer con encoding UTF-8*/
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(log_out_file), "UTF8"));
                writer.write(out_msg.replaceAll("<br>", "\n"));
                writer.close();
            }
        }catch (Exception e){
            out_msg += "Unable to write on log output file-"; 
        }
        return out_msg;
    }
    
    /**
     * Reads the input cvs file and return a Collection of files
     * @param csv_file CSV input file (separator ",")
     * @return Collection of files absolute path
     */
    private Collection parseCsvFile(File csv_file){
        String cache_content = "";
        try {
            FileReader file_reader = new FileReader(csv_file);
            BufferedReader buffer_reader = new BufferedReader(file_reader);
            String temp = "";
            //read file
            while ((temp = buffer_reader.readLine()) != null){
                cache_content += temp;            
            }
            buffer_reader.close();
          }
       catch (IOException e) {
            return null;
       }
       //gives back the collection
       return Arrays.asList(cache_content.split(","));            
   }
   
    /**
     * Reads the input xml file and return a Collection of files
     * @param xml_file XML input file 
     * @return Collection of files absolute path
     */
    private Collection parseXmlFile(File xml_file){
		List file_list = new ArrayList();
        try {
			SAXReader reader = new SAXReader();
			org.dom4j.Document document = reader.read(xml_file);
            List pdf_file_list = document.selectNodes("/filelist/file");
			for (int i = 0; pdf_file_list != null && i < pdf_file_list.size(); i++) {
				Node pdf_node = (Node) pdf_file_list.get(i);
				file_list.add(pdf_node.selectSingleNode("@value").getText().trim());
			}
		return file_list;
        }
       catch (Exception e) {
            return null;
       }
   }
   
   
   /**
    * Sets the Meta data "Creator" of the document
    * @param doc_to_set The document to set the creator
    */ 
   public static void setDocumentCreator(Document doc_to_set){
       doc_to_set.addCreator(MainConsole.CREATOR);
   }
   
   /**
    * Adds a listener to the list
    * @param wdl listener to add
    */
   public void addWorkDoneListener(WorkDoneListener wdl) {
       listeners.add(WorkDoneListener.class, wdl);
   }
   
   /**
    * Removes a listener to the list
    * @param wdl listener to remove
    */
   public void removeWorkDoneListener(WorkDoneListener wdl) {
       listeners.remove(WorkDoneListener.class, wdl);
   }
   
   /**
    * Tells the listeners that the percentage of work done has changed
    * @param wde Event
    */
   public void fireWorkDoneEvent(WorkDoneEvent wde)
   {
        Object[] listeners_list = listeners.getListenerList();
        // loop through each listener and pass on the event if needed
        for (int i = listeners_list.length-2; i>=0; i-=2){
             if (listeners_list[i]==WorkDoneListener.class)                 
             {
                 wde.dispatch((WorkDoneListener)listeners_list[i+1]);
                 //  ((WorkDoneListener)listeners_list[i]).percentageOfWorkDoneChanged(wde);
             }            
        }
   }
   
    private String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
}