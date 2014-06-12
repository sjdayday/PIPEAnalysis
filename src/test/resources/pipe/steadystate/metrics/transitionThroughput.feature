Feature: Calculating transition throughput metrics

  Scenario: Parsing a simple Petri net file
    Given I use the Petri net located at /simple.xml
    When I calculate the metrics
    Then I expect the transition throughputs to be
    """
         { "T0" : 0.5, "T1" : 0.5 }
    """

  Scenario: Parsing a simple differing rate Petri net file
    Given I use the Petri net located at /simple_uneven_rate.xml
    When I calculate the metrics
    Then I expect the transition throughputs to be
    """
         { "T0" : 0.66667, "T1" : 0.66667 }
    """

  Scenario: Parsing a simple vanishing Petri net file
    Given I use the Petri net located at /simple_vanishing.xml
    When I calculate the metrics
    Then I expect the transition throughputs to be
    """
         { "T5" : 0.0, "T6" : 0.0 }
    """

  Scenario: Parsing a cyclic vanishing Petri net file
    Given I use the Petri net located at /cyclic_vanishing.xml
    When I calculate the metrics
    Then I expect the transition throughputs to be
    """
         { "T0" : 0.0, "T1" : 0.0 }
    """

  Scenario: Parsing a 4 multi rate Petri net file
    Given I use the Petri net located at /4_multi_rate.xml
    When I calculate the metrics
    Then I expect the transition throughputs to be
    """
         { "T0" : 0.66667 , "T1" : 0.66667 ,
           "T2" : 0.75 , "T3" :  0.75 }
    """

  Scenario: Parsing a medium multi rate Petri net file
    Given I use the Petri net located at /16_multi_rate.xml
    When I calculate the metrics
    Then I expect the transition throughputs to be
    """
         { "T0" : 0.93334, "T1" : 0.93333,
           "T2" : 0.975, "T3" : 0.975 }
    """
