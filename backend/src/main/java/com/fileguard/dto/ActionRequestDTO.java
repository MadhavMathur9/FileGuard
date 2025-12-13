   package com.fileguard.dto;

import lombok.Data;   


@Data

public class ActionRequestDTO { 
    private String action;

    private boolean dryRun;
}
