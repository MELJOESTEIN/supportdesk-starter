import { httpResource } from '@angular/common/http';
import { Injectable } from '@angular/core';

import { environment } from '../../environments/environment';
import { AgentResume } from './agent.model';

/** Liste des agents, pour le sélecteur « assigné à ». Réservée aux agents côté serveur. */
@Injectable({ providedIn: 'root' })
export class AgentService {
  lister() {
    return httpResource<AgentResume[]>(() => `${environment.api}/agents`, { defaultValue: [] });
  }
}
