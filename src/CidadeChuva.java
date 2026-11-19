import java.util.Random;
import java.util.Scanner;

public class CidadeChuva {

    public static void main(String[] args) {

        // RM da equipe, parametros e geracao dos dados
        Scanner sc = new Scanner(System.in);
        System.out.println("Quantidade de integrantes (1 a 3):");
        int qtd = sc.nextInt();

        while (qtd < 1 || qtd > 3) {
            System.out.println("Valor invalido. Digite a quantidade de integrantes (1 a 3):");
            qtd = sc.nextInt();
        }

        int[] rms = new int[qtd];
        for (int i = 0; i < qtd; i++) {
            System.out.println("RM do integrante " + (i + 1) + ":");
            rms[i] = sc.nextInt();
        }
        int rm = calcularRmEquipe(rms);

        int[] p = calcularParametros(rm);
        int n = p[0];
        int m = p[1];
        int k = p[2];
        int l = p[3];
        int ordem = p[4];
        int[] deslocamentoLinha = criarDeslocamentoLinha(ordem);
        int[] deslocamentoColuna = criarDeslocamentoColuna(ordem);

        Random r = new Random(rm);
        int[][] altitude = gerarAltitude(r, n, m);
        int[][] lamina = gerarMatriz(r, n, m, 0, 10);
        int[][] laminaInicial = copiar(lamina);

        System.out.println("RM da equipe: " + rm);
        System.out.println("Parâmetros: N=" + n + " M=" + m + " K=" + k + " L=" + l + " ordem=" + ordem);
        imprimir(altitude, "Altitude (cm):");
        imprimir(laminaInicial, "Lâmina inicial (cm):");

       // simulacao e alerta
        System.out.println("Total de água no início: " + somar(lamina) + " cm");
        int passoAnalise = 2;
        int[][] laminaAnalise = null;
        for (int passo = 1; passo <= k; passo++) {
            int[][] anterior = lamina;
            int antes = somar(anterior);

            lamina = simularPasso(altitude, anterior, deslocamentoLinha, deslocamentoColuna);
            int depois = somar(lamina);

            System.out.println("Ciclo " + passo + ": total antes = " + antes + " cm, total depois = " + depois + " cm");
            anunciarAlertas(anterior, lamina, passo, l);
            if (passo == 1) {
                imprimir(lamina, "Lâmina após o 1º ciclo (cm):");
            }
            if (passo == passoAnalise) {
                laminaAnalise = copiar(lamina);
            }
        }
        imprimir(lamina, "Lâmina após o último ciclo (cm):");
        boolean[][] alerta = marcarAlerta(lamina, l);
        int totalAlerta = contarAlerta(alerta);
        System.out.println("Células em alerta (lamina >= " + l + "): " + totalAlerta);

        // relatorio da Defesa Civil
        imprimirMapaAlerta(alerta);

        int[] aguaPorLinha = calcularAguaPorLinha(lamina);
        System.out.println("Água por linha (cm):");
        for (int i = 0; i < n; i++) {
            System.out.println("Linha " + i + ": " + aguaPorLinha[i]);
        }
        int critica = encontrarLinhaCritica(aguaPorLinha);
        System.out.println("Linha crítica: " + critica + " (" + aguaPorLinha[critica] + " cm)");

        int[] regioes = contarRegioes(alerta);
        System.out.println("Regiões alagadas: " + regioes[0]);
        System.out.println("Tamanho da maior região: " + regioes[1] + " células");

        int a = rm % n;
        int b = (rm / 7) % m;
        int d1 = laminaAnalise[a][b];

        int[][] ingenua = copiar(laminaInicial);
        for (int passo = 1; passo <= passoAnalise; passo++) {
            ingenua = simularPassoIngenuo(altitude, ingenua, deslocamentoLinha, deslocamentoColuna);
        }
        int d2 = ingenua[a][b];

        System.out.println("Célula analisada: a=" + a + " b=" + b);
        System.out.println("D1 (versão correta, após " + passoAnalise + " passos): " + d1);
        System.out.println("D2 (versão ingênua, após " + passoAnalise + " passos): " + d2);

        System.out.println("RESUMO RM=" + rm);
        System.out.println("ALERTA=" + totalAlerta);
        System.out.println("CRITICA=" + critica + " AGUA=" + aguaPorLinha[critica]);
        System.out.println("D1=" + d1 + " D2=" + d2);
    }

    // soma os RMs de 1 a 3 integrantes para formar o RM da equipe.
    static int calcularRmEquipe(int[] rms) {
        int soma = 0;
        for (int i = 0; i < rms.length; i++) {
            soma += rms[i];
        }
        return soma;
    }

    // calcula N, M, K, L, ordem a partir do RM, sempre com divisao inteira.
    static int[] calcularParametros(int rm) {
        int[] p = new int[5];
        p[0] = 8 + rm % 3;
        p[1] = 10 + (rm / 10) % 3;
        p[2] = 4 + (rm / 100) % 3;
        p[3] = 10 + 2 * (rm % 4);
        p[4] = rm % 2;
        return p;
    }

    // gera a altitude do bairro como um vale (mais baixo no meio das linhas) com ruido de 0 a 4
    static int[][] gerarAltitude(Random r, int n, int m) {
        int[][] altitude = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                altitude[i][j] = 3 * Math.abs(i - n / 2) + r.nextInt(5);
            }
        }
        return altitude;
    }

    // cria uma matriz n x m com valores sorteados entre minimo e maximo
    static int[][] gerarMatriz(Random r, int n, int m, int minimo, int maximo) {
        int[][] x = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                x[i][j] = minimo + r.nextInt(maximo - minimo + 1);
            }
        }
        return x;
    }

    // monta o vetor com quanto somar na linha para chegar a cada vizinho (0 = N,L,S,O; 1 = O,S,L,N).
    static int[] criarDeslocamentoLinha(int ordem) {
        if (ordem == 0) {
            return new int[]{-1, 0, 1, 0};
        }
        return new int[]{0, 1, 0, -1};
    }

    // monta o vetor com quanto somar na coluna para chegar a cada vizinho (0 = N,L,S,O; 1 = O,S,L,N).
    static int[] criarDeslocamentoColuna(int ordem) {
        if (ordem == 0) {
            return new int[]{0, 1, 0, -1};
        }
        return new int[]{-1, 0, 1, 0};
    }

    // diz se a posicao (i, j) existe dentro de uma grade n x m.
    static boolean dentro(int i, int j, int n, int m) {
        return i >= 0 && i < n && j >= 0 && j < m;
    }

    // cria uma matriz nova com os mesmos valores (atribuir so copiaria a referencia).
    static int[][] copiar(int[][] matriz) {
        int[][] nova = new int[matriz.length][matriz[0].length];
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[0].length; j++) {
                nova[i][j] = matriz[i][j];
            }
        }
        return nova;
    }

    // imprime o titulo e a matriz, uma linha por println, valores separados por tabulacao
    static void imprimir(int[][] matriz, String titulo) {
        System.out.println(titulo);
        for (int i = 0; i < matriz.length; i++) {
            String linha = "";
            for (int j = 0; j < matriz[0].length; j++) {
                linha += matriz[i][j];
                if (j < matriz[0].length - 1) {
                    linha += "\t";
                }
            }
            System.out.println(linha);
        }
    }

    // soma todos os valores da matriz (usado para conferir a conservacao da agua)
    static int somar(int[][] matriz) {
        int soma = 0;
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[0].length; j++) {
                soma += matriz[i][j];
            }
        }
        return soma;
    }

    // procura o vizinho de menor nivel (altitude + lamina) lendo so a matriz recebida; devolve linha, coluna dele
    // ou null se nenhum vizinho tiver nivel menor que o da propria célula (empate vence o 1o examinado).
    static int[] encontrarDestino(int[][] altitude, int[][] lamina, int i, int j,
                                  int[] deslocamentoLinha, int[] deslocamentoColuna) {
        int n = lamina.length;
        int m = lamina[0].length;
        int menor = altitude[i][j] + lamina[i][j];
        int[] destino = null;
        for (int d = 0; d < 4; d++) {
            int vi = i + deslocamentoLinha[d];
            int vj = j + deslocamentoColuna[d];
            if (dentro(vi, vj, n, m)) {
                int nivel = altitude[vi][vj] + lamina[vi][vj];
                if (nivel < menor) {
                    menor = nivel;
                    destino = new int[]{vi, vj};
                }
            }
        }
        return destino;
    }

    // um passo da simulacao; le sempre da "fotografia" lamina e escreve so na copia nova.
    static int[][] simularPasso(int[][] altitude, int[][] lamina,
                                int[] deslocamentoLinha, int[] deslocamentoColuna) {
        int n = altitude.length;
        int m = altitude[0].length;
        int[][] nova = copiar(lamina);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                int t = lamina[i][j] / 2;
                if (t > 0) {
                    int[] destino = encontrarDestino(altitude, lamina, i, j, deslocamentoLinha, deslocamentoColuna);
                    if (destino != null) {
                        nova[i][j] -= t;
                        nova[destino[0]][destino[1]] += t;
                    }
                }
            }
        }
        return nova;
    }

    // versao "errada" do passo (questao D2): altera a propria matriz durante o percurso, sem fotografia,
    // entao t e os niveis ja enxergam a agua movida pelas celulas anteriores
    static int[][] simularPassoIngenuo(int[][] altitude, int[][] lamina,
                                       int[] deslocamentoLinha, int[] deslocamentoColuna) {
        int n = altitude.length;
        int m = altitude[0].length;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                int t = lamina[i][j] / 2;
                if (t > 0) {
                    int[] destino = encontrarDestino(altitude, lamina, i, j, deslocamentoLinha, deslocamentoColuna);
                    if (destino != null) {
                        lamina[i][j] -= t;
                        lamina[destino[0]][destino[1]] += t;
                    }
                }
            }
        }
        return lamina;
    }

    // marca true nas celulas cuja lamina e maior ou igual ao limiar de alerta (parametro L)
    static boolean[][] marcarAlerta(int[][] lamina, int limiarAlerta) {
        boolean[][] alerta = new boolean[lamina.length][lamina[0].length];
        for (int i = 0; i < lamina.length; i++) {
            for (int j = 0; j < lamina[0].length; j++) {
                alerta[i][j] = lamina[i][j] >= limiarAlerta;
            }
        }
        return alerta;
    }

    // ccnta quantas celulas estao em alerta
    static int contarAlerta(boolean[][] alerta) {
        int total = 0;
        for (int i = 0; i < alerta.length; i++) {
            for (int j = 0; j < alerta[0].length; j++) {
                if (alerta[i][j]) {
                    total++;
                }
            }
        }
        return total;
    }

    // compara a lamina antes e depois de um ciclo e avisa cada celula que acabou de cruzar o limiar L
    static void anunciarAlertas(int[][] anterior, int[][] atual, int ciclo, int limiarAlerta) {
        for (int i = 0; i < atual.length; i++) {
            for (int j = 0; j < atual[0].length; j++) {
                if (anterior[i][j] < limiarAlerta && atual[i][j] >= limiarAlerta) {
                    System.out.println("ALERTA: ciclo " + ciclo + ", célula (" + i + ", " + j + ") chegou a "
                            + atual[i][j] + " cm (limite " + limiarAlerta + " cm)");
                }
            }
        }
    }

    // imprime o mapa de alerta, "#" para alagado e "." para normal, separados por tabulacao
    static void imprimirMapaAlerta(boolean[][] alerta) {
        System.out.println("Mapa de alerta (# = alagado, . = normal):");
        for (int i = 0; i < alerta.length; i++) {
            String linha = "";
            for (int j = 0; j < alerta[0].length; j++) {
                if (alerta[i][j]) {
                    linha += "#";
                } else {
                    linha += ".";
                }
                if (j < alerta[0].length - 1) {
                    linha += "\t";
                }
            }
            System.out.println(linha);
        }
    }

    // soma a lamina de cada linha e devolve um vetor com um total por linha
    static int[] calcularAguaPorLinha(int[][] lamina) {
        int[] aguaPorLinha = new int[lamina.length];
        for (int i = 0; i < lamina.length; i++) {
            for (int j = 0; j < lamina[0].length; j++) {
                aguaPorLinha[i] += lamina[i][j];
            }
        }
        return aguaPorLinha;
    }

    // devolve o indice da linha com mais agua. No empate vale a de menor indice
    static int encontrarLinhaCritica(int[] aguaPorLinha) {
        int critica = 0;
        for (int i = 1; i < aguaPorLinha.length; i++) {
            if (aguaPorLinha[i] > aguaPorLinha[critica]) {
                critica = i;
            }
        }
        return critica;
    }

    // conta as regioes alagadas (grupos de celulas em alerta ligadas por N, L, S, O) e acha o tamanho da maior.
    // sem recursao ela usa uma pilha feita com dois vetores. Devolve {quantidade de regioes, tamanho da maior}
    static int[] contarRegioes(boolean[][] alerta) {
        int n = alerta.length;
        int m = alerta[0].length;
        boolean[][] visitada = new boolean[n][m];
        int[] pilhaI = new int[n * m];
        int[] pilhaJ = new int[n * m];
        int[] passoLinha = {-1, 0, 1, 0};
        int[] passoColuna = {0, 1, 0, -1};
        int regioes = 0;
        int maior = 0;

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                if (alerta[i][j] && !visitada[i][j]) {
                    regioes++;
                    int tamanho = 0;
                    int topo = 0;
                    pilhaI[topo] = i;
                    pilhaJ[topo] = j;
                    topo++;
                    visitada[i][j] = true;
                    while (topo > 0) {
                        topo--;
                        int ci = pilhaI[topo];
                        int cj = pilhaJ[topo];
                        tamanho++;
                        for (int d = 0; d < 4; d++) {
                            int vi = ci + passoLinha[d];
                            int vj = cj + passoColuna[d];
                            if (dentro(vi, vj, n, m) && alerta[vi][vj] && !visitada[vi][vj]) {
                                visitada[vi][vj] = true;
                                pilhaI[topo] = vi;
                                pilhaJ[topo] = vj;
                                topo++;
                            }
                        }
                    }
                    if (tamanho > maior) {
                        maior = tamanho;
                    }
                }
            }
        }
        return new int[]{regioes, maior};
    }
}