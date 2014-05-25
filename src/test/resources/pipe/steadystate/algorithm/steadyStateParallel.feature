Feature: steady state using parallel Jacobi

  Scenario: Parsing a simple Petri net file
    Given I use the Petri net located at /simple.xml
    When I calculate the steady state using a parallel jacobi solver
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


  Scenario: Parsing a simple vanishing Petri net file
    Given I use the Petri net located at /simple_vanishing.xml
    When I calculate the steady state using a parallel jacobi solver
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
    When I calculate the steady state using a parallel jacobi solver
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





