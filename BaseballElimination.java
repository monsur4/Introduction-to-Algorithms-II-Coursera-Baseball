import edu.princeton.cs.algs4.Bag;
import edu.princeton.cs.algs4.FlowEdge;
import edu.princeton.cs.algs4.FlowNetwork;
import edu.princeton.cs.algs4.FordFulkerson;
import edu.princeton.cs.algs4.In;
import edu.princeton.cs.algs4.Queue;
import edu.princeton.cs.algs4.ST;

public class BaseballElimination {
    private int numberOfTeams;
    private int[][] game;
    private int[] wins;
    private int[] losses;
    private int[] remaining;
    private ST<String, Integer> teamIndex;
    private ST<Integer, String> teamIndexToName;
    private Bag<String> teams;

    // create a baseball division from given filename in format specified below
    public BaseballElimination(String filename) {
        // read in the file
        In file = new In(filename);
        // set the number of teams
        numberOfTeams = Integer.parseInt(file.readLine());
        // intialize the iterable of teams
        teams = new Bag<>();
        // initialize the game matrix, wins, losses and remaining games
        game = new int[numberOfTeams][numberOfTeams];
        wins = new int[numberOfTeams];
        losses = new int[numberOfTeams];
        remaining = new int[numberOfTeams];

        // initialize teamIndex SymbolTable
        teamIndex = new ST<>();
        teamIndexToName = new ST<>();

        String line = file.readLine();
        int teamIndex = 0;
        while (line != null) {
            String[] lineSplit = line.trim().split("\\s+");
            String nameOfTeam = lineSplit[0];
            teams.add(nameOfTeam);
            teamIndexToName.put(teamIndex, nameOfTeam);
            int numberOfWins = Integer.parseInt(lineSplit[1]);
            wins[teamIndex] = numberOfWins;
            int numberOfLosses = Integer.parseInt(lineSplit[2]);
            losses[teamIndex] = numberOfLosses;
            int numberOfRemainingGames = Integer.parseInt(lineSplit[3]);
            remaining[teamIndex] = numberOfRemainingGames;

            for (int lineIndex = 4, i = 0; lineIndex < lineSplit.length; lineIndex++, i++) {
                game[teamIndex][i] = Integer.parseInt(lineSplit[lineIndex]);
            }

            // populate teamIndex symbolTable
            this.teamIndex.put(nameOfTeam, teamIndex);

            line = file.readLine();
            teamIndex++;
        }
    }

    // number of teams
    public int numberOfTeams() {
        return numberOfTeams;
    }

    // all teams
    public Iterable<String> teams() {
        return teams;
    }

    // number of wins for given team
    public int wins(String team) {
        validateTeam(team);
        return wins[teamIndex.get(team)];
    }

    // number of losses for given team
    public int losses(String team) {
        validateTeam(team);
        return losses[teamIndex.get(team)];
    }

    // number of remaining games for given team
    public int remaining(String team) {
        validateTeam(team);
        return remaining[teamIndex.get(team)];
    }

    // number of remaining games between team1 and team2
    public int against(String team1, String team2) {
        validateTeam(team1);
        validateTeam(team2);
        return game[teamIndex.get(team1)][teamIndex.get(team2)];
    }

    // is given team eliminated?
    public boolean isEliminated(String team) {
        validateTeam(team);
        return (boolean) solve(team)[0];
    }

    // subset R of teams that eliminates given team; null if not eliminated
    public Iterable<String> certificateOfElimination(String team) {
        validateTeam(team);
        return (Queue<String>) solve(team)[1];
    }

    private Object[] solve(String team) {
        int indexOfTeamInConcern = teamIndex.get(team);
        Queue<String> certificateOfElim = new Queue<>();

        // trivial elimination
        for (int t = 0; t < numberOfTeams; t++) {
            if (t == indexOfTeamInConcern) continue;
            if (wins[indexOfTeamInConcern] + remaining[indexOfTeamInConcern] < wins[t]) {
                certificateOfElim.enqueue(teamIndexToName.get(t));
                return new Object[] { true, certificateOfElim };
            }
        }

        int numberOfGameVerticesInNetwork =
                (numberOfTeams * numberOfTeams - numberOfTeams - (numberOfTeams - 1) - (
                        numberOfTeams - 1)) / 2;
        // create flow network ( number of vertices = source + gamevertex + team vertex + target
        int verticesInFlowNetwork = 1 + numberOfGameVerticesInNetwork + (numberOfTeams) + 1;
        FlowNetwork flowNetwork = new FlowNetwork(verticesInFlowNetwork);

        ST<String, Integer> vertexST = new ST<>();
        int numberOfGameVertex = 0;
        // source vertex
        vertexST.put("s", 0);
        vertexST.put("t", verticesInFlowNetwork - 1);
        int teamVertex = numberOfGameVerticesInNetwork + 1;
        for (int i = 0; i < numberOfTeams; i++) {
            for (int j = 0; j < numberOfTeams; j++) {
                if (j > i && j != indexOfTeamInConcern && i != indexOfTeamInConcern) {
                    String vertexString = i + "-" + j;
                    numberOfGameVertex++;
                    vertexST.put(vertexString, numberOfGameVertex);
                    FlowEdge flowEdgeFromSource = new FlowEdge(vertexST.get("s"),
                                                               vertexST.get(vertexString),
                                                               game[i][j]);
                    // for every edge from source to games between any two teams, add the edge to the flow network
                    flowNetwork.addEdge(flowEdgeFromSource);
                    // connect an edge from the game vertex to the teams involved
                    flowNetwork.addEdge(
                            new FlowEdge(vertexST.get(vertexString), i + teamVertex,
                                         Double.POSITIVE_INFINITY));
                    flowNetwork.addEdge(
                            new FlowEdge(vertexST.get(vertexString), j + teamVertex,
                                         Double.POSITIVE_INFINITY));
                }
            }

        }

        // add a edge from every team vertex to the target vertex
        for (int i = 0; i < numberOfTeams; i++) {
            if (i != indexOfTeamInConcern) {
                flowNetwork.addEdge(new FlowEdge(i + teamVertex, vertexST.get("t"),
                                                 wins[indexOfTeamInConcern]
                                                         + remaining[indexOfTeamInConcern]
                                                         - wins[i]));
            }
        }

        FordFulkerson fordFulkerson = new FordFulkerson(flowNetwork, vertexST.get("s"),
                                                        vertexST.get("t"));

        for (int i = teamVertex; i < teamVertex + numberOfTeams; i++) {
            if (i != indexOfTeamInConcern && fordFulkerson.inCut(i)) {
                certificateOfElim.enqueue(teamIndexToName.get(i % teamVertex));
            }
        }

        int maxCapacity = 0;

        for (FlowEdge e : flowNetwork.adj(vertexST.get("s"))) {
            maxCapacity += e.capacity();
        }
        if (fordFulkerson.value() == maxCapacity) {
            return new Object[] { false, null };
        }

        return new Object[] { true, certificateOfElim };
    }

    private void validateTeam(String team) {
        // if the teamIndex symbol table does not contain the team name, then throw the exception
        if (!teamIndex.contains(team)) {
            throw new IllegalArgumentException("An input to the method contains an invalid team");
        }
    }

    public static void main(String[] args) {
        BaseballElimination division = new BaseballElimination(args[0]);
        for (String team : division.teams()) {
            System.out.println(team);
        }
        System.out.println(division.isEliminated("Montreal"));
        System.out.println(division.certificateOfElimination("Montreal"));
    }
}
