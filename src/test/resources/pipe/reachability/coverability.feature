Feature: state space coverability graph of tangible states only

  @tangibleOnly
  Scenario: Parsing a simple Petri net file
    Given I use the Petri net located at /bause_unbound.xml
    When I generate the coverability graph sequentially
    Then I expect to see 11 state transitions
    ######### 1 #########
    And I expect a record with state
    """
       {"P0" : { "Default" : 1 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 0 } }
    """
    And successor
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 1 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 0 } }
    """
#    And rate 1.0

    ######### 2 #########
    And I expect a record with state
    """
       {"P0" : { "Default" : 1 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 0 } }
    """
    And successor
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 1 }, "P3" : { "Default" : 1 } }
    """
#    And rate 1.0

    ######### 3 #########
    And I expect a record with state
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 1 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 0 } }
    """
    And successor
    """
       {"P0" : { "Default" : 1 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 0 } }
    """
#    And rate 1.0

    ######### 4 #########
    And I expect a record with state
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 1 }, "P3" : { "Default" : 1 } }
    """
    And successor
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 1 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 1 } }
    """
#    And rate 1.0


    ######### 5 #########
    And I expect a record with state
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 1 }, "P3" : { "Default" : 1 } }
    """
    And successor
    """
       {"P0" : { "Default" : 1 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 0 } }
    """
#    And rate 1.0


    ######### 6 #########
    And I expect a record with state
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 1 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 1 } }
    """
    And successor
    """
       {"P0" : { "Default" : 1 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 2147483647 } }
    """
#    And rate 1.0


    ######### 7 #########
    And I expect a record with state
    """
       {"P0" : { "Default" : 1 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 2147483647 } }
    """
    And successor
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 1 }, "P3" : { "Default" : 2147483647 } }
    """
#    And rate 1.0

    ######### 8 #########
    And I expect a record with state
    """
       {"P0" : { "Default" : 1 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 2147483647 } }
    """
    And successor
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 1 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 2147483647 } }
    """
#    And rate 1.0

    ######### 9 #########
    And I expect a record with state
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 1 }, "P3" : { "Default" : 2147483647 } }
    """
    And successor
    """
       {"P0" : { "Default" : 1 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 2147483647 } }
    """
#    And rate 1.0


    ######### 10 #########
    And I expect a record with state
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 1 }, "P3" : { "Default" : 2147483647 } }
    """
    And successor
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 1 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 2147483647 } }
    """
#    And rate 1.0



    ######### 11 #########
    And I expect a record with state
    """
       {"P0" : { "Default" : 0 }, "P1" : { "Default" : 1 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 2147483647 } }
    """
    And successor
    """
       {"P0" : { "Default" : 1 }, "P1" : { "Default" : 0 }, "P2" : { "Default" : 0 }, "P3" : { "Default" : 2147483647 } }
    """
#    And rate 1.0


