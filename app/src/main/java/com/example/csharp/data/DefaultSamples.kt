package com.example.csharp.data

import com.example.csharp.model.CSharpProject

object DefaultSamples {

    private const val D = "$"

    val SAMPLES = listOf(
        CSharpProject(
            id = 1,
            title = "Olá Mundo & Interativo",
            description = "Exemplo básico com Console.WriteLine, Console.ReadLine e interpolação de strings.",
            code = """using System;

class Program
{
    static void Main()
    {
        Console.WriteLine("=== Bem-vindo ao C# Studio IDE! ===");
        Console.Write("Por favor, digite o seu nome: ");
        string nome = Console.ReadLine();
        
        Console.Write("Digite o seu ano de nascimento: ");
        string entradaAno = Console.ReadLine();
        int anoNascimento = int.Parse(entradaAno);
        
        int anoAtual = 2026;
        int idade = anoAtual - anoNascimento;
        
        Console.WriteLine(${D}"Olá, {nome}! Você tem aproximadamente {idade} anos.");
        Console.WriteLine(${D}"Execução em: {DateTime.Now}");
        Console.WriteLine("Tente adicionar um breakpoint na linha 14 e depurar!");
    }
}
""".trimIndent(),
            isSample = true
        ),
        CSharpProject(
            id = 2,
            title = "Fibonacci & Call Stack",
            description = "Demonstração de recursão e pilha de chamadas (Call Stack) passo a passo.",
            code = """using System;

class Program
{
    // Função recursiva para calcular Fibonacci
    static int CalcularFibonacci(int n)
    {
        // Pressione F11 (Step Into) para entrar nesta função
        if (n <= 0) return 0;
        if (n == 1) return 1;
        
        int termo1 = CalcularFibonacci(n - 1);
        int termo2 = CalcularFibonacci(n - 2);
        
        return termo1 + termo2;
    }

    static void Main()
    {
        Console.WriteLine("--- Sequência de Fibonacci ---");
        int termos = 7;
        
        for (int i = 0; i <= termos; i++)
        {
            int resultado = CalcularFibonacci(i);
            Console.WriteLine(${D}"Fibonacci({i}) = {resultado}");
        }
    }
}
""".trimIndent(),
            isSample = true
        ),
        CSharpProject(
            id = 3,
            title = "Ordenação Bubble Sort",
            description = "Algoritmo de ordenação com arrays e variáveis para inspecionar no depurador.",
            code = """using System;

class Program
{
    static void Main()
    {
        int[] numeros = { 64, 34, 25, 12, 22, 11, 90 };
        int n = numeros.Length;
        
        Console.WriteLine("Array original: " + numeros);
        
        // Ordenação Bubble Sort
        for (int i = 0; i < n - 1; i++)
        {
            for (int j = 0; j < n - i - 1; j++)
            {
                if (numeros[j] > numeros[j + 1])
                {
                    // Troca de elementos
                    int temp = numeros[j];
                    numeros[j] = numeros[j + 1];
                    numeros[j + 1] = temp;
                }
            }
        }
        
        Console.WriteLine("Array ordenado: " + numeros);
    }
}
""".trimIndent(),
            isSample = true
        ),
        CSharpProject(
            id = 4,
            title = "Conta Bancária & OOP",
            description = "Classes, campos, métodos e instanciação de objetos em C#.",
            code = """using System;

class ContaBancaria
{
    public string Titular;
    public double Saldo;

    public void Depositar(double valor)
    {
        Saldo += valor;
        Console.WriteLine(${D}"[Depósito] +R${D}{valor}. Novo saldo: R${D}{Saldo}");
    }

    public void Sacar(double valor)
    {
        if (valor <= Saldo)
        {
            Saldo -= valor;
            Console.WriteLine(${D}"[Saque] -R${D}{valor}. Novo saldo: R${D}{Saldo}");
        }
        else
        {
            Console.WriteLine(${D}"[Erro] Saldo insuficiente para sacar R${D}{valor}");
        }
    }
}

class Program
{
    static void Main()
    {
        ContaBancaria conta = new ContaBancaria();
        conta.Titular = "Maria Silva";
        conta.Saldo = 1500.0;
        
        Console.WriteLine(${D}"Conta aberta para: {conta.Titular}");
        Console.WriteLine(${D}"Saldo inicial: R${D}{conta.Saldo}");
        
        conta.Depositar(350.50);
        conta.Sacar(200.00);
        conta.Sacar(2000.00);
        
        Console.WriteLine(${D}"Saldo final de {conta.Titular}: R${D}{conta.Saldo}");
    }
}
""".trimIndent(),
            isSample = true
        ),
        CSharpProject(
            id = 5,
            title = "Coleção Dinâmica & Math",
            description = "Uso de List<T>, cálculos matemáticos com Math.Sqrt e filtros.",
            code = """using System;

class Program
{
    static void Main()
    {
        List<int> valores = new List<int>();
        
        // Preenche a lista com quadrados perfeitos
        for (int i = 1; i <= 6; i++)
        {
            valores.Add(i * i);
        }
        
        Console.WriteLine(${D}"Lista contém {valores.Count} elementos: {valores}");
        
        double soma = 0;
        foreach (var item in valores)
        {
            double raiz = Math.Sqrt(item);
            Console.WriteLine(${D}"Item: {item} | Raiz quadrada: {raiz}");
            soma += item;
        }
        
        double media = soma / valores.Count;
        Console.WriteLine(${D}"Soma total: {soma}");
        Console.WriteLine(${D}"Média aritmética: {media}");
    }
}
""".trimIndent(),
            isSample = true
        )
    )
}
