package com.stelligent.mini;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class AutomationDevops extends HttpServlet {

    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
        throws ServletException, IOException
    {
        PrintWriter writer = response.getWriter();
        writer.print("<h1>Automation for the People</h1>");
    }
}