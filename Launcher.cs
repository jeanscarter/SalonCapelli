using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;

namespace SalonCapelliLauncher
{
    static class Program
    {
        [STAThread]
        static void Main()
        {
            try
            {
                string projectDir = @"c:\Users\USUARIO\.gemini\antigravity-ide\scratch\SalonCapelli";
                string jarPath = Path.Combine(projectDir, @"target\SalonCapelli-uber.jar");

                if (!File.Exists(jarPath))
                {
                    MessageBox.Show(
                        "No se encontró el archivo ejecutable de la aplicación:\n" + jarPath,
                        "Error - Sistema Capelli",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Error
                    );
                    return;
                }

                string javawPath = FindJavaExecutable();
                if (string.IsNullOrEmpty(javawPath))
                {
                    MessageBox.Show(
                        "No se encontró una instalación de Java (javaw.exe) en su sistema.\n" +
                        "Por favor instale Java 21 o superior para ejecutar la aplicación.",
                        "Java No Encontrado - Sistema Capelli",
                        MessageBoxButtons.OK,
                        MessageBoxIcon.Error
                    );
                    return;
                }

                ProcessStartInfo psi = new ProcessStartInfo();
                psi.FileName = javawPath;
                psi.Arguments = string.Format("-Dfile.encoding=UTF-8 -jar \"{0}\"", jarPath);
                psi.WorkingDirectory = projectDir;
                psi.UseShellExecute = false;
                psi.CreateNoWindow = true;

                Process.Start(psi);
            }
            catch (Exception ex)
            {
                MessageBox.Show(
                    "Ocurrió un error al intentar iniciar la aplicación:\n\n" + ex.Message,
                    "Error al Iniciar - Sistema Capelli",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error
                );
            }
        }

        static string FindJavaExecutable()
        {
            // 1. Direct JDK 27 path
            string directJdk = @"C:\Program Files\Java\jdk-27\bin\javaw.exe";
            if (File.Exists(directJdk)) return directJdk;

            // 2. JAVA_HOME environment variable
            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (!string.IsNullOrEmpty(javaHome))
            {
                string candidate = Path.Combine(javaHome, @"bin\javaw.exe");
                if (File.Exists(candidate)) return candidate;
            }

            // 3. Search in Program Files\Java
            string programFiles = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
            string javaDir = Path.Combine(programFiles, "Java");
            if (Directory.Exists(javaDir))
            {
                foreach (string dir in Directory.GetDirectories(javaDir))
                {
                    string candidate = Path.Combine(dir, @"bin\javaw.exe");
                    if (File.Exists(candidate)) return candidate;
                }
            }

            // 4. Common Files Oracle javapath
            string oracleJava = Path.Combine(programFiles, @"Common Files\Oracle\Java\javapath\javaw.exe");
            if (File.Exists(oracleJava)) return oracleJava;

            // 5. PATH fallback
            string pathEnv = Environment.GetEnvironmentVariable("PATH");
            if (!string.IsNullOrEmpty(pathEnv))
            {
                foreach (string p in pathEnv.Split(';'))
                {
                    string trimmed = p.Trim();
                    if (!string.IsNullOrEmpty(trimmed))
                    {
                        try
                        {
                            string candidate = Path.Combine(trimmed, "javaw.exe");
                            if (File.Exists(candidate)) return candidate;
                        }
                        catch { }
                    }
                }
            }

            return null;
        }
    }
}
