Feature: steady state using asyncrhonous Gauss-Seidel

  Scenario: Parsing a simple Petri net file
    Given I use the Petri net located at /simple.xml
    When I calculate the steady state using a parallel gauss-seidel solver
    Then I expect a record for
    """
       { "P0" : { "Default" : 1 }, "P1" : { "Default" : 0 }}
    """
    And its probability to be 0.5
    And I expect another record for
    """
       { "P0" : { "Default" : 0 }, "P1" : { "Default" : 1 }}
    """
    And its probability to be 0.5


  Scenario: Parsing a simple differing rate Petri net file
    Given I use the Petri net located at /simple_uneven_rate.xml
    When I calculate the steady state using a parallel gauss-seidel solver
    Then I expect a record for
    """
       { "P0" : { "Default" : 1 }, "P1" : { "Default" : 0 }}
    """
    And its probability to be 0.3333333333333333
    And I expect another record for
    """
       { "P0" : { "Default" : 0 }, "P1" : { "Default" : 1 }}
    """
    And its probability to be 0.6666666666666666

  Scenario: Parsing a simple vanishing Petri net file
    Given I use the Petri net located at /simple_vanishing.xml
    When I calculate the steady state using a parallel gauss-seidel solver
    Then I expect a record for
    """
       { "1" : { "Default" : 1 }, "2" : { "Default" : 0 },
         "3" : { "Default" : 0 }, "4" : { "Default" : 0 },
         "5" : { "Default" : 0 }, "6" : { "Default" : 0 },
         "7" : { "Default" : 0 }, "8" : { "Default" : 0 }
       }
    """
    And its probability to be 0.0
    And I expect another record for
    """
       { "1" : { "Default" : 0 }, "2" : { "Default" : 0 },
         "3" : { "Default" : 0 }, "4" : { "Default" : 0 },
         "5" : { "Default" : 1 }, "6" : { "Default" : 0 },
         "7" : { "Default" : 0 }, "8" : { "Default" : 0 }
       }
    """
    And its probability to be 0.25
    And I expect another record for
    """
       { "1" : { "Default" : 0 }, "2" : { "Default" : 0 },
         "3" : { "Default" : 0 }, "4" : { "Default" : 0 },
         "5" : { "Default" : 0 }, "6" : { "Default" : 1 },
         "7" : { "Default" : 0 }, "8" : { "Default" : 0 }
       }
    """
    And its probability to be 0.25
    And I expect another record for
    """
       { "1" : { "Default" : 0 }, "2" : { "Default" : 0 },
         "3" : { "Default" : 0 }, "4" : { "Default" : 0 },
         "5" : { "Default" : 0 }, "6" : { "Default" : 0 },
         "7" : { "Default" : 1 }, "8" : { "Default" : 0 }
       }
    """
    And its probability to be 0.25

    And I expect another record for
    """
       { "1" : { "Default" : 0 }, "2" : { "Default" : 0 },
         "3" : { "Default" : 0 }, "4" : { "Default" : 0 },
         "5" : { "Default" : 0 }, "6" : { "Default" : 0 },
         "7" : { "Default" : 0 }, "8" : { "Default" : 1 }
       }
    """
    And its probability to be 0.25

  Scenario: Parsing a cylic vanishing Petri net
    Given I use the Petri net located at /cyclic_vanishing.xml
    When I calculate the steady state using a parallel gauss-seidel solver
    Then I expect a record for
    """
       { "1" : { "Default" : 1 }, "2" : { "Default" : 0 },
         "3" : { "Default" : 0 }, "4" : { "Default" : 0 },
         "5" : { "Default" : 0 }, "6" : { "Default" : 0 },
         "7" : { "Default" : 0 }, "8" : { "Default" : 0 }
       }
  """
    And its probability to be 0.0
    And I expect another record for
    """
       { "1" : { "Default" : 0 }, "2" : { "Default" : 0 },
         "3" : { "Default" : 0 }, "4" : { "Default" : 0 },
         "5" : { "Default" : 0 }, "6" : { "Default" : 1 },
         "7" : { "Default" : 0 }, "8" : { "Default" : 0 }
       }
  """
    And its probability to be 0.3333333333333333
    And I expect another record for
    """
       { "1" : { "Default" : 0 }, "2" : { "Default" : 0 },
         "3" : { "Default" : 0 }, "4" : { "Default" : 0 },
         "5" : { "Default" : 0 }, "6" : { "Default" : 0 },
         "7" : { "Default" : 1 }, "8" : { "Default" : 0 }
       }
  """
    And its probability to be 0.3333333333333333
    And I expect another record for
    """
       { "1" : { "Default" : 0 }, "2" : { "Default" : 0 },
         "3" : { "Default" : 0 }, "4" : { "Default" : 0 },
         "5" : { "Default" : 0 }, "6" : { "Default" : 0 },
         "7" : { "Default" : 0 }, "8" : { "Default" : 1 }
       }
    """
    And its probability to be 0.3333333333333333



Scenario: Parsing a small multi rate Petri net
    Given I use the Petri net located at /4_multi_rate.xml
    When I calculate the steady state using a parallel gauss-seidel solver
    Then I expect a record for
    """
       { "P0" : { "Default" : 1 }, "P1" : { "Default" : 0 },
         "P2" : { "Default" : 1 }, "P3" : { "Default" : 0 }
       }
    """
    And its probability to be 0.08333
    And I expect another record for
    """
       { "P0" : { "Default" : 1 }, "P1" : { "Default" : 0 },
         "P2" : { "Default" : 0 }, "P3" : { "Default" : 1 }
       }
    """
    And its probability to be 0.25
    And I expect another record for
    """
       { "P0" : { "Default" : 0 }, "P1" : { "Default" : 1 },
         "P2" : { "Default" : 1 }, "P3" : { "Default" : 0 }
       }
    """
    And its probability to be 0.16667
    And I expect another record for
    """
       { "P0" : { "Default" : 0 }, "P1" : { "Default" : 1 },
         "P2" : { "Default" : 0 }, "P3" : { "Default" : 1 }
       }
    """
    And its probability to be 0.5

  Scenario: Parsing a medium multi rate Petri net
    Given I use the Petri net located at /16_multi_rate.xml
    When I calculate the steady state using a parallel gauss-seidel solver
    #0
    Then I expect a record for
    """
       { "P0" : { "Default" : 3 }, "P1" : { "Default" : 0 },
         "P2" : { "Default" : 3 }, "P3" : { "Default" : 0 }
       }
    """
    And its probability to be 0.00167

    #1
    And I expect another record for
    """
       { "P0" : { "Default" : 3 }, "P1" : { "Default" : 0 },
         "P2" : { "Default" : 2 }, "P3" : { "Default" : 1 }
       }
    """
    And its probability to be 0.0.005

    #2
    And I expect another record for
    """
       { "P0" : { "Default" : 2 }, "P1" : { "Default" : 1 },
         "P2" : { "Default" : 3 }, "P3" : { "Default" : 0 }
       }
    """
    And its probability to be 0.00333

    #4
    And I expect another record for
    """
       { "P0" : { "Default" : 2 }, "P1" : { "Default" : 1 },
         "P2" : { "Default" : 2 }, "P3" : { "Default" : 1 }
       }
    """
    And its probability to be 0.1

    #5
    And I expect another record for
    """
       { "P0" : { "Default" : 1 }, "P1" : { "Default" : 2 },
         "P2" : { "Default" : 3 }, "P3" : { "Default" : 0 }
       }
    """
    And its probability to be 0.00667

    #6
    And I expect another record for
    """
       { "P0" : { "Default" : 3 }, "P1" : { "Default" : 0 },
         "P2" : { "Default" : 0 }, "P3" : { "Default" : 3 }
       }
    """
    And its probability to be 0.045

    #7
    And I expect another record for
    """
       { "P0" : { "Default" : 2 }, "P1" : { "Default" : 1 },
         "P2" : { "Default" : 1 }, "P3" : { "Default" : 2 }
       }
    """
    And its probability to be 0.03
    #8
    And I expect another record for
    """
       { "P0" : { "Default" : 1 }, "P1" : { "Default" : 2 },
         "P2" : { "Default" : 2 }, "P3" : { "Default" : 1 }
       }
    """
    And its probability to be 0.02

    #9
    And I expect another record for
    """
       { "P0" : { "Default" : 0 }, "P1" : { "Default" : 3 },
         "P2" : { "Default" : 3 }, "P3" : { "Default" : 0 }
       }
    """
    And its probability to be 0.01333

    #10
    And I expect another record for
    """
       { "P0" : { "Default" : 2 }, "P1" : { "Default" : 1 },
         "P2" : { "Default" : 0 }, "P3" : { "Default" : 3 }
       }
    """
    And its probability to be 0.09

    #11
    And I expect another record for
    """
       { "P0" : { "Default" : 1 }, "P1" : { "Default" : 2 },
         "P2" : { "Default" : 1 }, "P3" : { "Default" : 2 }
       }
    """
    And its probability to be 0.06

    #12
    And I expect another record for
    """
       { "P0" : { "Default" : 0 }, "P1" : { "Default" : 3 },
         "P2" : { "Default" : 2 }, "P3" : { "Default" : 1 }
       }
    """
    And its probability to be 0.04

    #13
    And I expect another record for
    """
       { "P0" : { "Default" : 1 }, "P1" : { "Default" : 2 },
         "P2" : { "Default" : 0 }, "P3" : { "Default" : 3 }
       }
    """
    And its probability to be 0.18

    #14
    And I expect another record for
    """
       { "P0" : { "Default" : 0 }, "P1" : { "Default" : 3 },
         "P2" : { "Default" : 1 }, "P3" : { "Default" : 2 }
       }
    """
    And its probability to be 0.12

    #15
    And I expect another record for
    """
       { "P0" : { "Default" : 0 }, "P1" : { "Default" : 3 },
         "P2" : { "Default" : 0 }, "P3" : { "Default" : 3 }
       }
    """
    And its probability to be 0.36



