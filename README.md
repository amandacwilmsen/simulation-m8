# Trabalho 1 – Simulação e Métodos Analíticos

**Integrantes:**  
Amanda Wilmsen  
Nicoli Siqueira

## Simulador para Rede de Filas

Este projeto corresponde ao primeiro trabalho da disciplina de **Simulação e Métodos Analíticos**.

O objetivo é implementar, em Java, um simulador de eventos discretos capaz de representar diferentes topologias de redes de filas. A configuração da simulação é carregada a partir de um arquivo `.yml`, permitindo definir filas, número de servidores, capacidades, intervalos de chegada e atendimento e probabilidades de roteamento entre as filas.

## Arquivos

- `Main.java`: código-fonte do simulador.
- `model.yml`: configuração da rede de filas utilizada na simulação.
- `README.md`: instruções de utilização do projeto.

## Requisitos

Para executar o simulador, é necessário ter o **Java JDK** instalado.

É possível verificar a instalação utilizando:

```bash
java -version
javac -version
```

## Como executar

No terminal, acesse a pasta onde estão os arquivos do projeto.

Compile o código:

```bash
javac Main.java
```

Em seguida, execute o simulador utilizando o arquivo de configuração:

```bash
java Main run model.yml
```

## Arquivo de configuração

O arquivo `model.yml` contém os parâmetros necessários para a simulação, incluindo:

- filas da rede;
- quantidade de servidores;
- capacidade das filas;
- intervalos de chegada;
- intervalos de atendimento;
- conexões entre as filas;
- probabilidades de roteamento;
- primeira chegada;
- seed utilizada;
- quantidade máxima de números aleatórios.

Dessa forma, diferentes topologias de redes de filas podem ser simuladas por meio da alteração do arquivo de configuração, sem necessidade de modificar o código-fonte.

## Modelo utilizado

Para validação do simulador, foi utilizada uma rede composta por três filas:

- **Q1:** G/G/1, chegadas entre 2 e 4 e atendimentos entre 1 e 2.
- **Q2:** G/G/2/5, atendimentos entre 4 e 6.
- **Q3:** G/G/2/10, atendimentos entre 5 e 15.

As filas iniciam vazias e a primeira chegada ocorre no tempo **2,0**.

A simulação é encerrada após o consumo de **100.000 números aleatórios**.

## Saída do simulador

Ao final da execução, o simulador apresenta, para cada fila:

- tempo acumulado em cada estado;
- distribuição de probabilidades dos estados;
- número de perdas de clientes.

Também é apresentado o **tempo global da simulação**.